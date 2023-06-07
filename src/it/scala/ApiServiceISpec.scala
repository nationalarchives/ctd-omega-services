import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Ref }
import io.circe._
import io.circe.parser._
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach, FutureOutcome }
import uk.gov.nationalarchives.omega.api.common.{ AppLogger, ErrorCode }
import uk.gov.nationalarchives.omega.api.common.ErrorCode.{ INVA002, INVA003, INVA005, INVA006, MISS002, MISS003, MISS005, MISS006 }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.messages.{ MessageProperties, OutgoingMessageType }
import uk.gov.nationalarchives.omega.api.services.ApiService

import javax.jms.{ Connection, MessageProducer, Session, TextMessage }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ApiServiceISpec
    extends FixtureAsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience with AppLogger
    with BeforeAndAfterEach with BeforeAndAfterAll {

  /** Note: Now that we have multiple scenarios, we see that we cannot run more than one at a time, likely because there
    * is contention with the JMS consumer.
    *
    * As such, the only way for the scenarios to pass (currently) is to run them individuals - and even then you'll
    * regularly see a failure.
    *
    * I think the whole approach to the reply message assertion needs to be improved.
    */
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(30, Seconds)), interval = scaled(Span(1, Seconds)))

  private val requestQueueName = "PACS001_request"
  private val replyQueueName = "PACE001_reply"
  private val sqsHostName = "localhost"
  private val sqsPort = 9324

  private val apiService = new ApiService(
    ServiceConfig(
      tempMessageDir = "temp",
      maxConsumers = 1,
      maxProducers = 1,
      maxDispatchers = 1,
      maxLocalQueueSize = 1,
      requestQueue = requestQueueName
    )
  )

  private val replyMessageText: Ref[IO, Option[String]] = Ref[IO].of(Option.empty[String]).unsafeRunSync()
  private val replyMessageId: Ref[IO, Option[String]] = Ref[IO].of(Option.empty[String]).unsafeRunSync()
  private val messageTypeId: Ref[IO, Option[String]] = Ref[IO].of(Option.empty[String]).unsafeRunSync()

  private val jmsClient = simpleQueueService.makeJmsClient[IO](
    Config(
      endpoint = simpleQueueService.Endpoint(Some(DirectAddress(HTTP, sqsHostName, Some(sqsPort))), "elasticmq"),
      credentials = Some(Credentials("x", "x")),
      clientId = simpleQueueService.ClientId("ctd-omega-services"),
      None
    )
  )

  case class ProducerAndSession(producer: MessageProducer, session: Session)

  override type FixtureParam = ProducerAndSession

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    val sqsTestConnector = SqsConnector(s"http://$sqsHostName:$sqsPort")
    val sqsTestConnection: Connection = sqsTestConnector.getConnection
    val session: Session = sqsTestConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val producer: MessageProducer = session.createProducer(session.createQueue(requestQueueName))
    super.withFixture(test.toNoArgAsyncTest(ProducerAndSession(producer, session)))
  }

  override def beforeAll(): Unit = {
    val consumerRes = for {
      client <- jmsClient
      consumer <-
        client.createAutoAcknowledgerConsumer(QueueName(replyQueueName), 1, 100.millis)
      _ <- Resource.eval(consumer.handle { (jmsMessage, _) =>
             for {
               _ <- readTextMessage(jmsMessage)
               _ <- readMessageTypeID(jmsMessage)
             } yield AutoAckAction.noOp
           })
    } yield consumer
    consumerRes.useForever.unsafeToFuture()
    apiService.start.unsafeToFuture()
    ()
  }

  override def afterAll(): Unit =
    Await.result(apiService.stop().unsafeToFuture(), 1.minute)

  override def afterEach(): Unit = {
    replyMessageText.set(Option.empty[String]).unsafeRunSync()
    replyMessageId.set(Option.empty[String]).unsafeRunSync()
    messageTypeId.set(Option.empty[String]).unsafeRunSync()
  }

  "The Message API" - {

    "returns an echo message when all fields are valid" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(contents = "Hello World!")

      sendMessage(f.session, f.producer, textMessageConfig)

      assertReplyMessage("The Echo Service says: Hello World!")

    }

    "returns legal statuses message when given a valid message type" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = Some("OSLISALS001"))

      sendMessage(f.session, f.producer, textMessageConfig)

      assertReplyMessage(s"""[
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/public-record",
    "name" : "Public Record"
  },
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/non-public-record",
    "name" : "Non-Public Record"
  },
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/public-record-unless-otherwise-stated",
    "name" : "Public Record (unless otherwise stated)"
  },
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/welsh-public-record",
    "name" : "Welsh Public Record"
  },
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/non-record-material",
    "name" : "Non-Record Material"
  }
]""".stripMargin)

    }

    "returns agent summaries message when given a valid ListAgentSummaryRequest" in { f =>
      val textMessageConfig = generateValidMessageConfig()
        .copy(messageTypeId = Some("OSLISAGT001"))
        .copy(contents = s"""{
                            |    "agentType" : ["CorporateBody","Person"],
                            |    "authorityFile" : false,
                            |    "depository" : false,
                            |    "version" : ""
                            |}""".stripMargin)

      sendMessage(f.session, f.producer, textMessageConfig)
      assertReplyMessage(agentSummariesExpectedResult)

    }
    "returns an echo message when the message body is" - {
      "empty (with padding)" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(contents = " ")

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage("The Echo Service says:  ")
      }
    }

    "returns an error message when" - {
      "the OMGMessageTypeID (aka SID)" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage(getExpectedJsonErrors(Map(MISS002 -> "Missing OMGMessageTypeID"))) *>
            assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)

        }

        "is unrecognised" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = Some("OSGESXXX100"))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage(getExpectedJsonErrors(Map(INVA002 -> "Invalid OMGMessageTypeID"))) *>
            assertMessageType(OutgoingMessageType.UnrecognisedMessageTypeError.entryName)

        }

      }
      "the OMGApplicationID" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(applicationId = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage(
            getExpectedJsonErrors(Map(MISS003 -> "Missing OMGApplicationID"))
          ) *>
            assertMessageType(OutgoingMessageType.InvalidApplicationError.entryName)

        }

        "isn't valid" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(applicationId = Some("ABC001"))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage(
            getExpectedJsonErrors(Map(INVA003 -> "Invalid OMGApplicationID"))
          ) *>
            assertMessageType(OutgoingMessageType.InvalidApplicationError.entryName)
        }

      }
    }
    "the OMGMessageFormat" - {
      "isn't provided" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(messageFormat = None)

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage(getExpectedJsonErrors(Map(MISS005 -> "Missing OMGMessageFormat"))) *>
          assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)

      }
      "isn't valid" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(messageFormat = Some("text/plain"))

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage(getExpectedJsonErrors(Map(INVA005 -> "Invalid OMGMessageFormat"))) *>
          assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)

      }
    }
    "the OMGToken" - {
      "isn't provided" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(token = None)

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage(getExpectedJsonErrors(Map(MISS006 -> "Missing OMGToken"))) *>
          assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)

      }
      "isn't valid" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(token = Some(" "))

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage(getExpectedJsonErrors(Map(INVA006 -> "Invalid OMGToken"))) *>
          assertMessageType(OutgoingMessageType.AuthenticationError.entryName)
      }
    }

  }

  private def generateValidMessageConfig(): TextMessageConfig =
    TextMessageConfig(
      contents = "Hello, World",
      messageTypeId = Some("OSGESZZZ100"),
      applicationId = Some("PACE001"),
      messageFormat = Some("application/json"),
      token = Some("AbCdEf123456"),
      replyAddress = Some("PACE001_reply")
    )

  private def asTextMessage(session: Session, messageConfig: TextMessageConfig): TextMessage = {
    val textMessage: TextMessage = session.createTextMessage(messageConfig.contents)
    messageConfig.messageTypeId.foreach { messageTypeId =>
      textMessage.setStringProperty(MessageProperties.OMGMessageTypeID, messageTypeId)
    }
    messageConfig.applicationId.foreach { applicationId =>
      textMessage.setStringProperty(MessageProperties.OMGApplicationID, applicationId)
    }
    messageConfig.messageFormat.foreach { messageFormat =>
      textMessage.setStringProperty(MessageProperties.OMGMessageFormat, messageFormat)
    }
    messageConfig.token.foreach { token =>
      textMessage.setStringProperty(MessageProperties.OMGToken, token)
    }
    messageConfig.replyAddress.foreach { replyAddress =>
      textMessage.setStringProperty(MessageProperties.OMGReplyAddress, replyAddress)
    }
    textMessage
  }

  private def readTextMessage(jmsMessage: JmsMessage): IO[Unit] =
    replyMessageId.set(jmsMessage.getJMSCorrelationId) *>
      jmsMessage.asTextF[IO].attempt.flatMap {
        case Right(text) =>
          replyMessageText.set(Some(text))
        case Left(e) => fail(s"Unable to read message contents due to ${e.getMessage}")
      }

  private def readMessageTypeID(jmsMessage: JmsMessage): IO[Unit] =
    messageTypeId.set(jmsMessage.getStringProperty(MessageProperties.OMGMessageTypeID))

  private def getExpectedJsonErrors(errorMap: Map[ErrorCode, String]): String = {
    val entries = errorMap.map { entry =>
      s"""
         |{
         |  "code" : "${entry._1}",
         |  "description" : "${entry._2}"
         |}
         |""".stripMargin
    }
    parse(s"[${entries.mkString(",")}]").getOrElse(Json.Null).toString
  }

  private def assertReplyMessage(expectedContents: String): IO[Assertion] =
    eventually {
      replyMessageText.get.asserting(_ mustBe Some(expectedContents))
    }

  private def assertMessageType(expectedMessageType: String): IO[Assertion] =
    eventually {
      messageTypeId.get.asserting(_ mustBe Some(expectedMessageType))
    }

  private def sendMessage(session: Session, producer: MessageProducer, textMessageConfig: TextMessageConfig): Unit =
    producer.send(asTextMessage(session, textMessageConfig))

  private def agentSummariesExpectedResult =
    s"""
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "3RX",
       |    "label" : "Abbot, Charles",
       |    "dateFrom" : "1798",
       |    "dateTo" : "1867"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "48N",
       |    "label" : "Baden-Powell, Lady Olave St Clair",
       |    "dateFrom" : "1889",
       |    "dateTo" : "1977"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "39K",
       |    "label" : "Cannon, John Francis Michael",
       |    "dateFrom" : "1930",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "3FH",
       |    "label" : "Dainton, Sir Frederick Sydney",
       |    "dateFrom" : "1914",
       |    "dateTo" : "1997"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "54J",
       |    "label" : "Edward, ",
       |    "dateFrom" : "1330",
       |    "dateTo" : "1376"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "2QX",
       |    "label" : "Edward VII",
       |    "dateFrom" : "1841",
       |    "dateTo" : "1910"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "561",
       |    "label" : "Fanshawe, Baron, of Richmond, ",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "46F",
       |    "label" : "Fawkes, Guy",
       |    "dateFrom" : "1570",
       |    "dateTo" : "1606"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "2JN",
       |    "label" : "George, David Lloyd",
       |    "dateFrom" : "1863",
       |    "dateTo" : "1945"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "34X",
       |    "label" : "Halley, Edmund",
       |    "dateFrom" : "1656",
       |    "dateTo" : "1742"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "2TK",
       |    "label" : "Halifax, ",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "39T",
       |    "label" : "Irvine, Linda Mary",
       |    "dateFrom" : "1928",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "4",
       |    "label" : "Jack the Ripper, ",
       |    "dateFrom" : "1888",
       |    "dateTo" : "1888"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "4FF",
       |    "label" : "Keay, Sir Lancelot Herman",
       |    "dateFrom" : "1883",
       |    "dateTo" : "1974"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "ST",
       |    "label" : "Lawson, Nigel",
       |    "dateFrom" : "1932",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "51X",
       |    "label" : "Macpherson, Sir William (Alan)",
       |    "dateFrom" : "1926",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "515",
       |    "label" : "Newcastle, 1st Duke of, ",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "4VF",
       |    "label" : "Old Pretender, The",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "4H3",
       |    "label" : "Oliphant, Sir Mark Marcus Laurence Elwin",
       |    "dateFrom" : "1901",
       |    "dateTo" : "2000"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "46W",
       |    "label" : "Paine, Thomas",
       |    "dateFrom" : "1737",
       |    "dateTo" : "1809"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "3SH",
       |    "label" : "Reade, Hubert Granville Revell",
       |    "dateFrom" : "1859",
       |    "dateTo" : "1938"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "2TF",
       |    "label" : "Reading, ",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "53T",
       |    "label" : "Salisbury, Sir Edward James",
       |    "dateFrom" : "1886",
       |    "dateTo" : "1978"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "3QL",
       |    "label" : "Tate, Sir Henry",
       |    "dateFrom" : "1819",
       |    "dateTo" : "1899"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "37K",
       |    "label" : "Uvarov, Sir Boris Petrovitch",
       |    "dateFrom" : "1889",
       |    "dateTo" : "1970"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "2T1",
       |    "label" : "Vane-Tempest-Stewart, Charles Stewart",
       |    "dateFrom" : "1852",
       |    "dateTo" : "1915"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "4RW",
       |    "label" : "Victor Amadeus, ",
       |    "dateFrom" : "1666",
       |    "dateTo" : "1732"
       |  },
       |  {
       |    "agentType" : "Person",
       |    "identifier" : "3GY",
       |    "label" : "Victoria, ",
       |    "dateFrom" : "1819",
       |    "dateTo" : "1901"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "RR6",
       |    "label" : "100th (Gordon Highlanders) Regiment of Foot",
       |    "dateFrom" : "1794",
       |    "dateTo" : "1794"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "S34",
       |    "label" : "1st Regiment of Foot or Royal Scots",
       |    "dateFrom" : "1812",
       |    "dateTo" : "1812"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "87K",
       |    "label" : "Abbotsbury Railway Company",
       |    "dateFrom" : "1877",
       |    "dateTo" : "1877"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VWG",
       |    "label" : "Accountant General in the Court of Chancery",
       |    "dateFrom" : "1726",
       |    "dateTo" : "1726"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "LWY",
       |    "label" : "Admiralty Administrative Whitley Council, General Purposes Committee",
       |    "dateFrom" : "1942",
       |    "dateTo" : "1942"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VS6",
       |    "label" : "Advisory Committee on Animal Feedingstuffs",
       |    "dateFrom" : "1999",
       |    "dateTo" : "1999"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "CC",
       |    "label" : "Bank of England",
       |    "dateFrom" : "1694",
       |    "dateTo" : "1694"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "N9S",
       |    "label" : "Bank on Tickets of the Million Adventure",
       |    "dateFrom" : "1695",
       |    "dateTo" : "1695"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "JS8",
       |    "label" : "BBC",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "8WG",
       |    "label" : "Bee Husbandry Committee",
       |    "dateFrom" : "1959",
       |    "dateTo" : "1959"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "6VQ",
       |    "label" : "Cabinet",
       |    "dateFrom" : "1919",
       |    "dateTo" : "1919"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "SV",
       |    "label" : "Cabinet",
       |    "dateFrom" : "1945",
       |    "dateTo" : "1945"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "5V4",
       |    "label" : "Cabinet, Committee for Control of Official Histories",
       |    "dateFrom" : "1946",
       |    "dateTo" : "1946"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "GW5",
       |    "label" : "Cattle Emergency Committee",
       |    "dateFrom" : "1934",
       |    "dateTo" : "1934"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "934",
       |    "label" : "Dairy Crest",
       |    "dateFrom" : "1981",
       |    "dateTo" : "1981"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "9HC",
       |    "label" : "Dean of the Chapel Royal",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "WGL",
       |    "label" : "Department for Environment, Food and Rural Affairs, Water Quality Division",
       |    "dateFrom" : "2002",
       |    "dateTo" : "2002"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "WJ4",
       |    "label" : "Department for Exiting the European Union",
       |    "dateFrom" : "2016",
       |    "dateTo" : "2016"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "9YJ",
       |    "label" : "East Grinstead, Groombridge and Tunbridge Wells Railway Company",
       |    "dateFrom" : "1862",
       |    "dateTo" : "1862"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "HF4",
       |    "label" : "East India Company",
       |    "dateFrom" : "1600",
       |    "dateTo" : "1600"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "WN3",
       |    "label" : "Education and Skills Funding Agency",
       |    "dateFrom" : "2017",
       |    "dateTo" : "2017"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "WNL",
       |    "label" : "Education and Skills Funding Agency",
       |    "dateFrom" : "2017",
       |    "dateTo" : "2017"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "Q1R",
       |    "label" : "Falkland Islands Company",
       |    "dateFrom" : "1899",
       |    "dateTo" : "1899"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "SQ9",
       |    "label" : "Fish's Corps of Foot",
       |    "dateFrom" : "1782",
       |    "dateTo" : "1782"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "R6R",
       |    "label" : "Foreign and Commonwealth Office, Consulate, Dusseldorf, West Germany",
       |    "dateFrom" : "1968",
       |    "dateTo" : "1968"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "HKL",
       |    "label" : "Foreign Office, Consulate, Angora and Konieh, Ottoman Empire",
       |    "dateFrom" : "1895",
       |    "dateTo" : "1895"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "KSC",
       |    "label" : "Gaming Board for Great Britain",
       |    "dateFrom" : "1968",
       |    "dateTo" : "1968"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "73R",
       |    "label" : "GCHQ",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VR1",
       |    "label" : "Geffrye Museum",
       |    "dateFrom" : "1914",
       |    "dateTo" : "1914"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "QX5",
       |    "label" : "General Nursing Council for England and Wales, Registration and Enrolment Committee",
       |    "dateFrom" : "1970",
       |    "dateTo" : "1970"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "C1Y",
       |    "label" : "Halifax High Level and North and South Junction Railway Company",
       |    "dateFrom" : "1884",
       |    "dateTo" : "1884"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "W2T",
       |    "label" : "Hansard Society",
       |    "dateFrom" : "1944",
       |    "dateTo" : "1944"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "F18",
       |    "label" : "Health and Safety Commission, Health and Safety Executive, Employment Medical Advisory Service",
       |    "dateFrom" : "1975",
       |    "dateTo" : "1975"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "8JK",
       |    "label" : "Her Majesty's Stationery Office",
       |    "dateFrom" : "1986",
       |    "dateTo" : "1986"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "9FV",
       |    "label" : "Ideal Benefit Society",
       |    "dateFrom" : "1912",
       |    "dateTo" : "1912"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "5YX",
       |    "label" : "Imperial War Museum: Churchill Museum and Cabinet War Rooms",
       |    "dateFrom" : "1939",
       |    "dateTo" : "1939"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "W1Q",
       |    "label" : "Independent Expert Group on Mobile Phones",
       |    "dateFrom" : "1999",
       |    "dateTo" : "1999"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "QLY",
       |    "label" : "Independent Expert Group on Mobile Phones",
       |    "dateFrom" : "1999",
       |    "dateTo" : "1999"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "LS5",
       |    "label" : "Jodrell Bank Observatory, Cheshire",
       |    "dateFrom" : "1955",
       |    "dateTo" : "1955"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "92W",
       |    "label" : "Joint Milk Quality Committee",
       |    "dateFrom" : "1948",
       |    "dateTo" : "1948"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "L3W",
       |    "label" : "Justices in Eyre, of Assize, of Gaol Delivery, of Oyer and Terminer, of the Peace, and of Nisi Prius",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "N8X",
       |    "label" : "Justices of the Forest",
       |    "dateFrom" : "1166",
       |    "dateTo" : "1166"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "THY",
       |    "label" : "Kew Gardens Archive",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "SGX",
       |    "label" : "King's Own Dragoons, 1751-1818",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "CCR",
       |    "label" : "Knitting, Lace and Net Industry Training Board",
       |    "dateFrom" : "1966",
       |    "dateTo" : "1966"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TTT",
       |    "label" : "King's Volunteers Regiment of Foot, 1761-1763",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VR7",
       |    "label" : "Lady Lever Art Gallery",
       |    "dateFrom" : "1922",
       |    "dateTo" : "1922"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "XQ",
       |    "label" : "Law Society",
       |    "dateFrom" : "1825",
       |    "dateTo" : "1825"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "91W",
       |    "label" : "League of Mercy",
       |    "dateFrom" : "1898",
       |    "dateTo" : "1898"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VX",
       |    "label" : "Legal Aid Board",
       |    "dateFrom" : "1989",
       |    "dateTo" : "1989"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TXG",
       |    "label" : "Legal Aid Board, 1988-1989",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "6LL",
       |    "label" : "Machinery of Government Committee",
       |    "dateFrom" : "1917",
       |    "dateTo" : "1917"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "G6N",
       |    "label" : "Magnetic Department",
       |    "dateFrom" : "1839",
       |    "dateTo" : "1839"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "71K",
       |    "label" : "Manpower Distribution Board",
       |    "dateFrom" : "1916",
       |    "dateTo" : "1916"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "KN1",
       |    "label" : "Master of the Rolls Archives Committee",
       |    "dateFrom" : "1925",
       |    "dateTo" : "1925"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "J6X",
       |    "label" : "National Agricultural Advisory Service, Great House Experimental Husbandry Farm",
       |    "dateFrom" : "1951",
       |    "dateTo" : "1951"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "K7N",
       |    "label" : "National Air Traffic Control Services, Director General Projects and Engineering, Directorate of Projects",
       |    "dateFrom" : "1963",
       |    "dateTo" : "1963"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TSL",
       |    "label" : "National Archives, The",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "LSN",
       |    "label" : "Navy Board, Transport Branch, Prisoner of War Department",
       |    "dateFrom" : "1817",
       |    "dateTo" : "1817"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "W1S",
       |    "label" : "Office for Budget Responsibility",
       |    "dateFrom" : "2010",
       |    "dateTo" : "2010"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "N4W",
       |    "label" : "Office of Population Censuses and Surveys, Computer Division",
       |    "dateFrom" : "1972",
       |    "dateTo" : "1972"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "QQC",
       |    "label" : "Office of Works, Directorate of Works, Maintenance Surveyors Division, Sanitary Engineers Section",
       |    "dateFrom" : "1928",
       |    "dateTo" : "1928"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "QFY",
       |    "label" : "Office of the President of Social Security Appeal Tribunals, Medical Appeal Tribunals and Vaccine Damage Tribunals",
       |    "dateFrom" : "1984",
       |    "dateTo" : "1984"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VYJ",
       |    "label" : "Ordnance Survey of Great Britain, Directorate of Data Collection and Management",
       |    "dateFrom" : "2003",
       |    "dateTo" : "2003"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "8FX",
       |    "label" : "Overseas Development Administration, Information Department",
       |    "dateFrom" : "1970",
       |    "dateTo" : "1970"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "3C",
       |    "label" : "Overseas Finance, International Finance, IF1 International Financial Institutions and Debt Division",
       |    "dateFrom" : "1990",
       |    "dateTo" : "1990"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "988",
       |    "label" : "Oxford University Archives",
       |    "dateFrom" : "1634",
       |    "dateTo" : "1634"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TWX",
       |    "label" : "Oxford University Press",
       |    "dateFrom" : "1633",
       |    "dateTo" : "1633"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "79L",
       |    "label" : "Palace Court",
       |    "dateFrom" : "1660",
       |    "dateTo" : "1660"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TX6",
       |    "label" : "Parker Inquiry",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VY4",
       |    "label" : "Paymaster General of the Court of Chancery, Supreme Court Pay Office",
       |    "dateFrom" : "1884",
       |    "dateTo" : "1884"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VX3",
       |    "label" : "Persona Associates Ltd",
       |    "dateFrom" : "1989",
       |    "dateTo" : "1989"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "V36",
       |    "label" : "Petty Bag Office",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "8R6",
       |    "label" : "Queen Anne's Bounty",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "SH2",
       |    "label" : "Queen's Own Dragoons, 1788-1818",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "79X",
       |    "label" : "Queens Prison",
       |    "dateFrom" : "1842",
       |    "dateTo" : "1842"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "W91",
       |    "label" : "Queen's Printer for Scotland",
       |    "dateFrom" : "1999",
       |    "dateTo" : "1999"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "F11",
       |    "label" : "Radioactive Substances Advisory Committee",
       |    "dateFrom" : "1948",
       |    "dateTo" : "1948"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "CYY",
       |    "label" : "Railway Executive",
       |    "dateFrom" : "1947",
       |    "dateTo" : "1947"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "CXY",
       |    "label" : "Railway Executive",
       |    "dateFrom" : "1914",
       |    "dateTo" : "1914"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "CY1",
       |    "label" : "Railway Executive",
       |    "dateFrom" : "1939",
       |    "dateTo" : "1939"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TXH",
       |    "label" : "SaBRE",
       |    "dateFrom" : "2002",
       |    "dateTo" : "2002"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "739",
       |    "label" : "Scaccarium Superius",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "NWN",
       |    "label" : "School of Anti-Aircraft Artillery",
       |    "dateFrom" : "1942",
       |    "dateTo" : "1942"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "SGS",
       |    "label" : "Scots Greys, 1877-1921",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VXR",
       |    "label" : "Takeover Panel",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "QQR",
       |    "label" : "Tate Gallery",
       |    "dateFrom" : "1897",
       |    "dateTo" : "1897"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "63K",
       |    "label" : "Tate Gallery Archive",
       |    "dateFrom" : "1970",
       |    "dateTo" : "1970"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "G91",
       |    "label" : "Thalidomide Y List Inquiry",
       |    "dateFrom" : "1978",
       |    "dateTo" : "1978"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "FKS",
       |    "label" : "The Buying Agency",
       |    "dateFrom" : "1991",
       |    "dateTo" : "1991"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "JLC",
       |    "label" : "The Crown Estate, Other Urban Estates, Foreshore and Seabed Branches",
       |    "dateFrom" : "1973",
       |    "dateTo" : "1973"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "SYL",
       |    "label" : "Uhlans Britanniques de Sainte-Domingue (Charmilly's), 1794-1795",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "TXK",
       |    "label" : "UK Passport Service",
       |    "dateFrom" : "1991",
       |    "dateTo" : "1991"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "V3H",
       |    "label" : "UK Web Archiving Consortium",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "CCX",
       |    "label" : "United Kingdom Atomic Energy Authority, Atomic Weapons Research Establishment, Directors Office",
       |    "dateFrom" : "1954",
       |    "dateTo" : "1954"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VTY",
       |    "label" : "Valuation Office Agency",
       |    "dateFrom" : "1991",
       |    "dateTo" : "19910"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "9HJ",
       |    "label" : "Venetian Republic",
       |    "dateFrom" : "727",
       |    "dateTo" : "727"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "QYF",
       |    "label" : "Victoria and Albert Museum",
       |    "dateFrom" : "1857",
       |    "dateTo" : "1857"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "61H",
       |    "label" : "Victoria & Albert Museum, Archive of Art and Design",
       |    "dateFrom" : "1992",
       |    "dateTo" : "1992"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "W9K",
       |    "label" : "Wales Tourist Board",
       |    "dateFrom" : "1969",
       |    "dateTo" : "1969"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "VRG",
       |    "label" : "Walker Art Gallery",
       |    "dateFrom" : "1873",
       |    "dateTo" : "1873"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "61J",
       |    "label" : "Wallace Collection",
       |    "dateFrom" : "1897",
       |    "dateTo" : "1897"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "HXV",
       |    "label" : "War and Colonial Department, Commissioners for liquidating the Danish and Dutch loans for St Thomas and St John",
       |    "dateFrom" : "1808",
       |    "dateTo" : "1808"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "V2R",
       |    "label" : "Zahid Mubarek Inquiry",
       |    "dateFrom" : "2004",
       |    "dateTo" : "2004"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "763",
       |    "label" : "Zambia Department, Commonwealth Office",
       |    "dateFrom" : "1967",
       |    "dateTo" : "1967"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "765",
       |    "label" : "Zambia, Malawi and Southern Africa Department",
       |    "dateFrom" : "1968",
       |    "dateTo" : "1968"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "G2Y",
       |    "label" : "Zuckerman Working Party",
       |    "dateFrom" : "",
       |    "dateTo" : ""
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "63F",
       |    "label" : "British Museum Central Archive",
       |    "dateFrom" : "2001",
       |    "dateTo" : "2001"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "614",
       |    "label" : "British Library, Sound Archive",
       |    "dateFrom" : "1983",
       |    "dateTo" : "1983"
       |  },
       |  {
       |    "agentType" : "CorporateBody",
       |    "identifier" : "S2",
       |    "label" : "The National Archives",
       |    "dateFrom" : "2003",
       |    "dateTo" : ""
       |  }
       |]
       |""".stripMargin
}

case class TextMessageConfig(
  contents: String,
  messageTypeId: Option[String],
  applicationId: Option[String],
  messageFormat: Option[String],
  token: Option[String],
  replyAddress: Option[String]
)
