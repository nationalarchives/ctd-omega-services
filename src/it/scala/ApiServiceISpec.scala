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
                            |    "type" : ["CorporateBody","Person"],
                            |    "authority-file" : false,
                            |    "depository" : false,
                            |    "version-timestamp" : ""
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
    s"""[
  {
    "type" : "Person",
    "identifier" : "3RX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "3RX",
        "label" : "Abbot, Charles",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1798",
        "date-to" : "1867",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "48N",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "48N",
        "label" : "Baden-Powell, Lady Olave St Clair",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1889",
        "date-to" : "1977",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "39K",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "39K",
        "label" : "Cannon, John Francis Michael",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1930",
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "3FH",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "3FH",
        "label" : "Dainton, Sir Frederick Sydney",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1914",
        "date-to" : "1997",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "54J",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "54J",
        "label" : "Edward, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1330",
        "date-to" : "1376",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "2QX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "2QX",
        "label" : "Edward VII",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1841",
        "date-to" : "1910",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "561",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "561",
        "label" : "Fanshawe, Baron, of Richmond, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "46F",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "46F",
        "label" : "Fawkes, Guy",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1570",
        "date-to" : "1606",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "2JN",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "2JN",
        "label" : "George, David Lloyd",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1863",
        "date-to" : "1945",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "34X",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "34X",
        "label" : "Halley, Edmund",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1656",
        "date-to" : "1742",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "2TK",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "2TK",
        "label" : "Halifax, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "39T",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "39T",
        "label" : "Irvine, Linda Mary",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1928",
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "4",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "4",
        "label" : "Jack the Ripper, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1888",
        "date-to" : "1888",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "4FF",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "4FF",
        "label" : "Keay, Sir Lancelot Herman",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1883",
        "date-to" : "1974",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "ST",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "ST",
        "label" : "Lawson, Nigel",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1932",
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "51X",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "51X",
        "label" : "Macpherson, Sir William (Alan)",
        "authority-file" : false,
        "depository" : false,
        "date-from" : "1926",
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "515",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "515",
        "label" : "Newcastle, 1st Duke of, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "4VF",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "4VF",
        "label" : "Old Pretender, The",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "4H3",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "4H3",
        "label" : "Oliphant, Sir Mark Marcus Laurence Elwin",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1901",
        "date-to" : "2000",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "46W",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "46W",
        "label" : "Paine, Thomas",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1737",
        "date-to" : "1809",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "3SH",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "3SH",
        "label" : "Reade, Hubert Granville Revell",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1859",
        "date-to" : "1938",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "2TF",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "2TF",
        "label" : "Reading, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "53T",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "53T",
        "label" : "Salisbury, Sir Edward James",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1886",
        "date-to" : "1978",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "3QL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "3QL",
        "label" : "Tate, Sir Henry",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1819",
        "date-to" : "1899",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "37K",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "37K",
        "label" : "Uvarov, Sir Boris Petrovitch",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1889",
        "date-to" : "1970",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "2T1",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "2T1",
        "label" : "Vane-Tempest-Stewart, Charles Stewart",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1852",
        "date-to" : "1915",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "4RW",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "4RW",
        "label" : "Victor Amadeus, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1666",
        "date-to" : "1732",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "3GY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "3GY",
        "label" : "Victoria, ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1819",
        "date-to" : "1901",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "RR6",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "RR6",
        "label" : "100th (Gordon Highlanders) Regiment of Foot",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1794",
        "date-to" : "1794",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "S34",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "S34",
        "label" : "1st Regiment of Foot or Royal Scots",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1812",
        "date-to" : "1812",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "87K",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "87K",
        "label" : "Abbotsbury Railway Company",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1877",
        "date-to" : "1877",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VWG",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VWG",
        "label" : "Accountant General in the Court of Chancery",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1726",
        "date-to" : "1726",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "LWY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "LWY",
        "label" : "Admiralty Administrative Whitley Council, General Purposes Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1942",
        "date-to" : "1942",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VS6",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VS6",
        "label" : "Advisory Committee on Animal Feedingstuffs",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1999",
        "date-to" : "1999",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "CC",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "CC",
        "label" : "Bank of England",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1694",
        "date-to" : "1694",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "N9S",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "N9S",
        "label" : "Bank on Tickets of the Million Adventure",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1695",
        "date-to" : "1695",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "JS8",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "JS8",
        "label" : "BBC",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "8WG",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "8WG",
        "label" : "Bee Husbandry Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1959",
        "date-to" : "1959",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "6VQ",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "6VQ",
        "label" : "Cabinet",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1919",
        "date-to" : "1919",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "SV",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "SV",
        "label" : "Cabinet",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1945",
        "date-to" : "1945",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "5V4",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "5V4",
        "label" : "Cabinet, Committee for Control of Official Histories",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1946",
        "date-to" : "1946",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "GW5",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "GW5",
        "label" : "Cattle Emergency Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1934",
        "date-to" : "1934",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "934",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "934",
        "label" : "Dairy Crest",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1981",
        "date-to" : "1981",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "9HC",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "9HC",
        "label" : "Dean of the Chapel Royal",
        "authority-file" : false,
        "depository" : false,
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "WGL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "WGL",
        "label" : "Department for Environment, Food and Rural Affairs, Water Quality Division",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2002",
        "date-to" : "2002",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "WJ4",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "WJ4",
        "label" : "Department for Exiting the European Union",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2016",
        "date-to" : "2016",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "9YJ",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "9YJ",
        "label" : "East Grinstead, Groombridge and Tunbridge Wells Railway Company",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1862",
        "date-to" : "1862",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "HF4",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "HF4",
        "label" : "East India Company",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1600",
        "date-to" : "1600",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "WN3",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "WN3",
        "label" : "Education and Skills Funding Agency",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2017",
        "date-to" : "2017",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "WNL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "WNL",
        "label" : "Education and Skills Funding Agency",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2017",
        "date-to" : "2017",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "Q1R",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "Q1R",
        "label" : "Falkland Islands Company",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1899",
        "date-to" : "1899",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "SQ9",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "SQ9",
        "label" : "Fish's Corps of Foot",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1782",
        "date-to" : "1782",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "R6R",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "R6R",
        "label" : "Foreign and Commonwealth Office, Consulate, Dusseldorf, West Germany",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1968",
        "date-to" : "1968",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "HKL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "HKL",
        "label" : "Foreign Office, Consulate, Angora and Konieh, Ottoman Empire",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1895",
        "date-to" : "1895",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "KSC",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "KSC",
        "label" : "Gaming Board for Great Britain",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1968",
        "date-to" : "1968",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "73R",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "73R",
        "label" : "GCHQ",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VR1",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VR1",
        "label" : "Geffrye Museum",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1914",
        "date-to" : "1914",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "QX5",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "QX5",
        "label" : "General Nursing Council for England and Wales, Registration and Enrolment Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1970",
        "date-to" : "1970",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "C1Y",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "C1Y",
        "label" : "Halifax High Level and North and South Junction Railway Company",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1884",
        "date-to" : "1884",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "W2T",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "W2T",
        "label" : "Hansard Society",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1944",
        "date-to" : "1944",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "F18",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "F18",
        "label" : "Health and Safety Commission, Health and Safety Executive, Employment Medical Advisory Service",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1975",
        "date-to" : "1975",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "8JK"[info]         {
,
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "8JK",
        "label" : "Her Majesty's Stationery Office",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1986",
        "date-to" : "1986",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "9FV",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "9FV",
        "label" : "Ideal Benefit Society",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1912",
        "date-to" : "1912",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "5YX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "5YX",
        "label" : "Imperial War Museum: Churchill Museum and Cabinet War Rooms",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1939",
        "date-to" : "1939",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "W1Q",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "W1Q",
        "label" : "Independent Expert Group on Mobile Phones",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1999",
        "date-to" : "1999",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "QLY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "QLY",
        "label" : "Independent Expert Group on Mobile Phones",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1999",
        "date-to" : "1999",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "LS5",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "LS5",
        "label" : "Jodrell Bank Observatory, Cheshire",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1955",
        "date-to" : "1955",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "92W",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "92W",
        "label" : "Joint Milk Quality Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1948",
        "date-to" : "1948",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "L3W",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "L3W",
        "label" : "Justices in Eyre, of Assize, of Gaol Delivery, of Oyer and Terminer, of the Peace, and of Nisi Prius",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "N8X",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "N8X",
        "label" : "Justices of the Forest",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1166",
        "date-to" : "1166",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "THY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "THY",
        "label" : "Kew Gardens Archive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "SGX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "SGX",
        "label" : "King's Own Dragoons, 1751-1818",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "CCR",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "CCR",
        "label" : "Knitting, Lace and Net Industry Training Board",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1966",
        "date-to" : "1966",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TTT",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TTT",
        "label" : "King's Volunteers Regiment of Foot, 1761-1763",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VR7",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VR7",
        "label" : "Lady Lever Art Gallery",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1922",
        "date-to" : "1922",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "XQ",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "XQ",
        "label" : "Law Society",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1825",
        "date-to" : "1825",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "91W",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "91W",
        "label" : "League of Mercy",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1898",
        "date-to" : "1898",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VX",
        "label" : "Legal Aid Board",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1989",
        "date-to" : "1989",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TXG",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TXG",
        "label" : "Legal Aid Board, 1988-1989",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "6LL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "6LL",
        "label" : "Machinery of Government Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1917",
        "date-to" : "1917",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "G6N",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "G6N",
        "label" : "Magnetic Department",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1839",
        "date-to" : "1839",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "71K",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "71K",
        "label" : "Manpower Distribution Board",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1916",
        "date-to" : "1916",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "KN1",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "KN1",
        "label" : "Master of the Rolls Archives Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1925",
        "date-to" : "1925",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "J6X",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "J6X",
        "label" : "National Agricultural Advisory Service, Great House Experimental Husbandry Farm",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1951",
        "date-to" : "1951",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "K7N",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "K7N",
        "label" : "National Air Traffic Control Services, Director General Projects and Engineering, Directorate of Projects",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1963",
        "date-to" : "1963",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TSL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TSL",
        "label" : "National Archives, The",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "LSN",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "LSN",
        "label" : "Navy Board, Transport Branch, Prisoner of War Department",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1817",
        "date-to" : "1817",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "W1S",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "W1S",
        "label" : "Office for Budget Responsibility",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2010",
        "date-to" : "2010",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "N4W",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "N4W",
        "label" : "Office of Population Censuses and Surveys, Computer Division",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1972",
        "date-to" : "1972",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "QQC",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "QQC",
        "label" : "Office of Works, Directorate of Works, Maintenance Surveyors Division, Sanitary Engineers Section",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1928",
        "date-to" : "1928",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "QFY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "QFY",
        "label" : "Office of the President of Social Security Appeal Tribunals, Medical Appeal Tribunals and Vaccine Damage Tribunals",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1984",
        "date-to" : "1984",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VYJ",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VYJ",
        "label" : "Ordnance Survey of Great Britain, Directorate of Data Collection and Management",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2003",
        "date-to" : "2003",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "8FX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "8FX",
        "label" : "Overseas Development Administration, Information Department",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1970",
        "date-to" : "1970",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "3C",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "3C",
        "label" : "Overseas Finance, International Finance, IF1 International Financial Institutions and Debt Division",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1990",
        "date-to" : "1990",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "988",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "988",
        "label" : "Oxford University Archives",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1634",
        "date-to" : "1634",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TWX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TWX",
        "label" : "Oxford University Press",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1633",
        "date-to" : "1633",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "79L",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "79L",
        "label" : "Palace Court",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1660",
        "date-to" : "1660",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TX6",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TX6",
        "label" : "Parker Inquiry",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VY4",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VY4",
        "label" : "Paymaster General of the Court of Chancery, Supreme Court Pay Office",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1884",
        "date-to" : "1884",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VX3",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VX3",
        "label" : "Persona Associates Ltd",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1989",
        "date-to" : "1989",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "V36",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "V36",
        "label" : "Petty Bag Office",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "8R6",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "8R6",
        "label" : "Queen Anne's Bounty",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "SH2",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "SH2",
        "label" : "Queen's Own Dragoons, 1788-1818",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "79X",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "79X",
        "label" : "Queens Prison",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1842",
        "date-to" : "1842",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "W91",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "W91",
        "label" : "Queen's Printer for Scotland",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1999",
        "date-to" : "1999",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "F11",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "F11",
        "label" : "Radioactive Substances Advisory Committee",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1948",
        "date-to" : "1948",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "CYY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "CYY",
        "label" : "Railway Executive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1947",
        "date-to" : "1947",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "CXY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "CXY",
        "label" : "Railway Executive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1914",
        "date-to" : "1914",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "CY1",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "CY1",
        "label" : "Railway Executive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1939",
        "date-to" : "1939",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TXH",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TXH",
        "label" : "SaBRE",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2002",
        "date-to" : "2002",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "739",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "739",
        "label" : "Scaccarium Superius",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "NWN",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "NWN",
        "label" : "School of Anti-Aircraft Artillery",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1942",
        "date-to" : "1942",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "SGS",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "SGS",
        "label" : "Scots Greys, 1877-1921",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VXR",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VXR",
        "label" : "Takeover Panel",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "QQR",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "QQR",
        "label" : "Tate Gallery",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1897",
        "date-to" : "1897",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "63K",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "63K",
        "label" : "Tate Gallery Archive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1970",
        "date-to" : "1970",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "G91",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "G91",
        "label" : "Thalidomide Y List Inquiry",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1978",
        "date-to" : "1978",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "FKS",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "FKS",
        "label" : "The Buying Agency",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1991",
        "date-to" : "1991",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "JLC",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "JLC",
        "label" : "The Crown Estate, Other Urban Estates, Foreshore and Seabed Branches",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1973",
        "date-to" : "1973",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "SYL",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "SYL",
        "label" : "Uhlans Britanniques de Sainte-Domingue (Charmilly's), 1794-1795",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "TXK",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "TXK",
        "label" : "UK Passport Service",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1991",
        "date-to" : "1991",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "V3H",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "V3H"[info]           
        "label" : "UK Web Archiving Consortium",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1930",
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "CCX",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "CCX",
        "label" : "United Kingdom Atomic Energy Authority, Atomic Weapons Research Establishment, Directors Office",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1954",
        "date-to" : "1954",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VTY",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VTY",
        "label" : "Valuation Office Agency",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1991",
        "date-to" : "19910",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "9HJ",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "9HJ",
        "label" : "Venetian Republic",
        "authority-file" : false,
        "depository" : false,
        "date-from" : "727",
        "date-to" : "727",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "QYF",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "QYF",
        "label" : "Victoria and Albert Museum",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1857",
        "date-to" : "1857",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "61H",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "61H",
        "label" : "Victoria & Albert Museum, Archive of Art and Design",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1992",
        "date-to" : "1992",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "W9K",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "W9K",
        "label" : "Wales Tourist Board",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1969",
        "date-to" : "1969",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "VRG",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "VRG",
        "label" : "Walker Art Gallery",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1873",
        "date-to" : "1873",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "61J",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "61J",
        "label" : "Wallace Collection",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1897",
        "date-to" : "1897",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "HXV",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "HXV",
        "label" : "War and Colonial Department, Commissioners for liquidating the Danish and Dutch loans for St Thomas and St John",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1808",
        "date-to" : "1808",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "V2R",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "V2R",
        "label" : "Zahid Mubarek Inquiry",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2004",
        "date-to" : "2004",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "763",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "763",
        "label" : "Zambia Department, Commonwealth Office",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1967",
        "date-to" : "1967",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "765",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "765",
        "label" : "Zambia, Malawi and Southern Africa Department",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1968",
        "date-to" : "1968",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "G2Y",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "G2Y",
        "label" : "Zuckerman Working Party",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : null,
        "date-to" : null,
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "63F",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "63F",
        "label" : "British Museum Central Archive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2001",
        "date-to" : "2001",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "614",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "614",
        "label" : "British Library, Sound Archive",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1983",
        "date-to" : "1983",
        "previous-description" : null
      }
    ]
  },
  {
    "type" : "CorporateBody",
    "identifier" : "S2",
    "current-description" : "current description",
    "description" : [
      {
        "identifier" : "S2",
        "label" : "The National Archives",
        "authority-file" : false,
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2003",
        "date-to" : null,
        "previous-description" : null
      }
    ]
  }
] """.stripMargin
}

case class TextMessageConfig(
  contents: String,
  messageTypeId: Option[String],
  applicationId: Option[String],
  messageFormat: Option[String],
  token: Option[String],
  replyAddress: Option[String]
)
