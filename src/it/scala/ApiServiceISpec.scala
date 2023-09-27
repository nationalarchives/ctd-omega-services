import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Ref }
import io.circe._
import io.circe.parser._
import jms4s.JmsAutoAcknowledgerConsumer
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach, FutureOutcome }
import uk.gov.nationalarchives.omega.api.common.ErrorCode.{ INVA002, INVA003, INVA005, INVA006, MISS002, MISS003, MISS005, MISS006 }
import uk.gov.nationalarchives.omega.api.common.{ AppLogger, ErrorCode }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.messages.{ MessageProperties, OutgoingMessageType }
import uk.gov.nationalarchives.omega.api.repository.vocabulary.Cat
import uk.gov.nationalarchives.omega.api.services.ApiService

import javax.jms.{ Connection, MessageProducer, Session, TextMessage }
import scala.concurrent.duration.DurationInt
import scala.io.Source

class ApiServiceISpec
    extends FixtureAsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience with AppLogger
    with BeforeAndAfterEach with BeforeAndAfterAll {

  private val requestQueueName = "PACS001_request"
  private val replyQueueName = "PACE001_reply"
  private val sqsHostName = "localhost"
  private val sqsPort = 9324

  private val serviceConfig = ServiceConfig(
    tempMessageDir = "temp",
    maxConsumers = 1,
    maxProducers = 1,
    maxDispatchers = 1,
    maxLocalQueueSize = 1,
    requestQueue = requestQueueName,
    sparqlEndpoint = BulkLoadData.testRepositoryUrl
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

  case class ApiServiceFixture(
    producer: MessageProducer,
    session: Session,
    apiService: ApiService,
    comsumerRes: Resource[IO, JmsAutoAcknowledgerConsumer[IO]]
  )

  override type FixtureParam = ApiServiceFixture

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    val sqsTestConnector = SqsConnector(s"http://$sqsHostName:$sqsPort")
    val sqsTestConnection: Connection = sqsTestConnector.getConnection
    val session: Session = sqsTestConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val producer: MessageProducer = session.createProducer(session.createQueue(requestQueueName))
    val apiService = new ApiService(serviceConfig)
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
    super.withFixture(test.toNoArgAsyncTest(ApiServiceFixture(producer, session, apiService, consumerRes)))
  }

  override def beforeAll(): Unit =
    BulkLoadData.createRepository().unsafeRunSync()

  override def afterEach(): Unit = {
    replyMessageText.set(Option.empty[String]).unsafeRunSync()
    replyMessageId.set(Option.empty[String]).unsafeRunSync()
    messageTypeId.set(Option.empty[String]).unsafeRunSync()
  }

  /** Each test has the same sequence of events:-
    *   1. Start the JMS consumer resource (to listen for message replies) 2. Start the API service 3. Send a test
    *      message 4. Assert the reply message is as expected 5. Shutdown the API service 6. Shutdown the JMS consumer
    *      resource
    */

  "The Message API" - {

    "returns an echo message when all fields are valid" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(contents = "Hello World!")
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage("The Echo Service says: Hello World!"))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }

    "returns legal statuses message when given a valid message type" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = Some("OSLISALS001"))
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result <- Resource.liftK(assertReplyMessage(s"""[
  {
    "identifier" : "${Cat.publicRecord}",
    "label" : "Public Record"
  },
  {
    "identifier" : "${Cat.nonPublicRecord}",
    "label" : "Non-Public Record"
  },
  {
    "identifier" : "${Cat.publicRecordUnlessOtherwiseStated}",
    "label" : "Public Record (unless otherwise stated)"
  },
  {
    "identifier" : "${Cat.welshPublicRecord}",
    "label" : "Welsh Public Record"
  },
  {
    "identifier" : "${Cat.nonRecordMaterial}",
    "label" : "Non-Record Material"
  }
]""".stripMargin))
        _ <- Resource.eval(apiServiceFiber.cancel)
        _ <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }

    "returns a full record when a request is sent with the record concept URI" in { f =>
      val textMessageConfig = generateValidMessageConfig()
        .copy(messageTypeId = Some("OSGEFREC001"))
        .copy(contents = s"""{
                            |    "identifier" : "${Cat.NS}COAL.2022.N36R.P"
                            |}""".stripMargin)
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage(getExpectedRecordFull))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
  }

  "returns agent summaries message when the given ListAgentSummaryRequest has" - {
    " multiple agent types" in { f =>
      val textMessageConfig = generateValidMessageConfig()
        .copy(messageTypeId = Some("OSLISAGT001"))
        .copy(contents = s"""{
                            |    "type" : ["Corporate Body", "Person"]
                            |}""".stripMargin)
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage(agentSummariesExpectedLatest))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
    " multiple agent types and all versions" in { f =>
      val textMessageConfig = generateValidMessageConfig()
        .copy(messageTypeId = Some("OSLISAGT001"))
        .copy(contents = s"""{
                            |    "type" : ["Corporate Body", "Person"],
                            |    "version-timestamp" : "all"
                            |}""".stripMargin)
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage(agentSummariesExpectedAll))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
    " an agent type and the depository" in { f =>
      val textMessageConfig = generateValidMessageConfig()
        .copy(messageTypeId = Some("OSLISAGT001"))
        .copy(contents = s"""{
                            |    "type" : ["Corporate Body"],
                            |    "depository" : true
                            |}""".stripMargin)
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage(agentPlaceOfDepositSummariesExpectedResult))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }

    // TODO Since it's not possible to send an empty message in SQS the empty payload cannot be tested and
    // TODO this test is ignored as it fails on decode of spaces.

    " an empty payload (with padding)" ignore { f =>
      val textMessageConfig = generateValidMessageConfig()
        .copy(contents = " ")
        .copy(messageTypeId = Some("OSLISAGT001"))
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage(agentSummariesExpectedLatest))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
  }
  "returns an echo message when the message body is" - {
    "empty (with padding)" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(contents = " ")
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result          <- Resource.liftK(assertReplyMessage("The Echo Service says:  "))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
  }

  "returns an error message when" - {
    "the OMGMessageTypeID (aka SID)" - {
      "isn't provided" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = None)
        val serviceIO = f.apiService.startSuspended
        val res = for {
          consumerFiber   <- f.comsumerRes.start
          apiServiceFiber <- Resource.liftK(serviceIO.start)
          _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
          result <- Resource.liftK(
                      assertReplyMessage(
                        getExpectedJsonErrors(Map(MISS002 -> "Missing OMGMessageTypeID"))
                      ) *> assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)
                    )
          _ <- Resource.eval(apiServiceFiber.cancel)
          _ <- consumerFiber.cancel
        } yield result
        res.use(assert => IO.pure(assert))
      }

      "is unrecognised" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = Some("OSGESXXX100"))
        val serviceIO = f.apiService.startSuspended
        val res = for {
          consumerFiber   <- f.comsumerRes.start
          apiServiceFiber <- Resource.liftK(serviceIO.start)
          _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
          result <- Resource.liftK(
                      assertReplyMessage(
                        getExpectedJsonErrors(Map(INVA002 -> "Invalid OMGMessageTypeID"))
                      ) *> assertMessageType(OutgoingMessageType.UnrecognisedMessageTypeError.entryName)
                    )
          _ <- Resource.eval(apiServiceFiber.cancel)
          _ <- consumerFiber.cancel
        } yield result
        res.use(assert => IO.pure(assert))
      }

    }
    "the OMGApplicationID" - {
      "isn't provided" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(applicationId = None)
        val serviceIO = f.apiService.startSuspended
        val res = for {
          consumerFiber   <- f.comsumerRes.start
          apiServiceFiber <- Resource.liftK(serviceIO.start)
          _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
          result <- Resource.liftK(
                      assertReplyMessage(
                        getExpectedJsonErrors(Map(MISS003 -> "Missing OMGApplicationID"))
                      ) *> assertMessageType(OutgoingMessageType.InvalidApplicationError.entryName)
                    )
          _ <- Resource.eval(apiServiceFiber.cancel)
          _ <- consumerFiber.cancel
        } yield result
        res.use(assert => IO.pure(assert))
      }

      "isn't valid" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(applicationId = Some("ABC001"))
        val serviceIO = f.apiService.startSuspended
        val res = for {
          consumerFiber   <- f.comsumerRes.start
          apiServiceFiber <- Resource.liftK(serviceIO.start)
          _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
          result <- Resource.liftK(
                      assertReplyMessage(
                        getExpectedJsonErrors(Map(INVA003 -> "Invalid OMGApplicationID"))
                      ) *> assertMessageType(OutgoingMessageType.InvalidApplicationError.entryName)
                    )
          _ <- Resource.eval(apiServiceFiber.cancel)
          _ <- consumerFiber.cancel
        } yield result
        res.use(assert => IO.pure(assert))
      }

    }
  }
  "the OMGMessageFormat" - {
    "isn't provided" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(messageFormat = None)
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result <- Resource.liftK(
                    assertReplyMessage(
                      getExpectedJsonErrors(Map(MISS005 -> "Missing OMGMessageFormat"))
                    ) *> assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)
                  )
        _ <- Resource.eval(apiServiceFiber.cancel)
        _ <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
    "isn't valid" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(messageFormat = Some("text/plain"))
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result <-
          Resource.liftK(
            assertReplyMessage(getExpectedJsonErrors(Map(INVA005 -> "Invalid OMGMessageFormat"))) *> assertMessageType(
              OutgoingMessageType.InvalidMessageFormatError.entryName
            )
          )
        _ <- Resource.eval(apiServiceFiber.cancel)
        _ <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
  }
  "the OMGToken" - {
    "isn't provided" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(token = None)
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result <- Resource.liftK(
                    assertReplyMessage(getExpectedJsonErrors(Map(MISS006 -> "Missing OMGToken"))) *> assertMessageType(
                      OutgoingMessageType.InvalidMessageFormatError.entryName
                    )
                  )
        _ <- Resource.eval(apiServiceFiber.cancel)
        _ <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
    "isn't valid" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(token = Some(" "))
      val serviceIO = f.apiService.startSuspended
      val res = for {
        consumerFiber   <- f.comsumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        _               <- Resource.liftK(sendMessage(f.session, f.producer, textMessageConfig))
        result <- Resource.liftK(
                    assertReplyMessage(getExpectedJsonErrors(Map(INVA006 -> "Invalid OMGToken"))) *> assertMessageType(
                      OutgoingMessageType.AuthenticationError.entryName
                    )
                  )
        _ <- Resource.eval(apiServiceFiber.cancel)
        _ <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
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

  private def sendMessage(session: Session, producer: MessageProducer, textMessageConfig: TextMessageConfig): IO[Unit] =
    IO.blocking(producer.send(asTextMessage(session, textMessageConfig)))

  private def agentSummariesExpectedLatest = {
    val summaries = Source.fromResource("expected-agent-summaries-latest.json").getLines().mkString
    parse(summaries) match {
      case Right(json) => json.printWith(Printer.spaces2)
      case Left(_)     => ""
    }
  }

  private def agentSummariesExpectedAll = {
    val summaries = Source.fromResource("expected-agent-summaries-all.json").getLines().mkString
    parse(summaries) match {
      case Right(json) => json.printWith(Printer.spaces2)
      case Left(_)     => ""
    }
  }

  private def agentPlaceOfDepositSummariesExpectedResult = {
    val summaries = Source.fromResource("expected-place-of-deposit-summaries.json").getLines().mkString
    parse(summaries) match {
      case Right(json) => json.printWith(Printer.spaces2)
      case Left(_)     => ""
    }
  }

  private def getExpectedRecordFull =
    s"""{
       |  "identifier" : "${Cat.NS}COAL.2022.N36R.P",
       |  "type" : "Physical",
       |  "creator" : [
       |    "${Cat.NS}agent.24"
       |  ],
       |  "current-description" : "${Cat.NS}COAL.2022.N36R.P.1",
       |  "description" : [
       |    {
       |      "identifier" : "${Cat.NS}COAL.2022.N36R.P.1",
       |      "secondary-identifier" : [
       |        {
       |          "identifier" : "COAL 80/2055/22",
       |          "type" : "${Cat.NS}classicCatalogueReference"
       |        }
       |      ],
       |      "label" : "<scopecontent><p>Coal News. Model II storage unit for photograph negatives (strips of 4). Photograph negatives Nos. T3060-T3102. </p></scopecontent>",
       |      "abstract" : "<scopecontent><p>Coal News. Model II storage unit for photograph negatives (strips of 4). Photograph negatives Nos. T3060-T3102. </p></scopecontent>",
       |      "access-rights" : [
       |        "${Cat.NS}policy.Open_Description",
       |        "${Cat.NS}policy.Normal_Closure_before_FOI_Act_30_years_from_1964-10-31"
       |      ],
       |      "is-part-of" : [
       |        "${Cat.NS}recordset.COAL.2022.2831"
       |      ],
       |      "previous-sibling" : "${Cat.NS}COAL.2022.N361.P.1",
       |      "version-timestamp" : "2022-12-05T20:37:31.28Z",
       |      "asset-legal-status" : {
       |        "identifier" : "${Cat.NS}public-record",
       |        "label" : "Public Record"
       |      },
       |      "legacy-tna-cs13-record-type" : "Item",
       |      "created" : {
       |        "description" : "[1964 October]",
       |        "temporal" : {
       |          "date-from" : "1964-09-30Z",
       |          "date-to" : "1964-10-31Z"
       |        }
       |      },
       |      "archivists-note" : "[Grid reference: N/A]",
       |      "source-of-acquisition" : "${Cat.NS}agent.24",
       |      "subject" : [
       |        {
       |          "identifier" : "${Cat.NS}agent.24",
       |          "label" : "from 1965"
       |        }
       |      ]
       |    }
       |  ]
       |}""".stripMargin
}

case class TextMessageConfig(
  contents: String,
  messageTypeId: Option[String],
  applicationId: Option[String],
  messageFormat: Option[String],
  token: Option[String],
  replyAddress: Option[String]
)
