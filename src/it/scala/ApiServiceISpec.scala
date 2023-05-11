import cats.effect.{ IO, Ref }
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
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
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.services.ApiService
import io.circe._
import io.circe.parser._
import uk.gov.nationalarchives.omega.api.common.ErrorCode
import uk.gov.nationalarchives.omega.api.common.ErrorCode.{ BLAN001, INVA002, INVA003, INVA005, INVA006, INVA007, MISS002, MISS003, MISS005, MISS006, MISS007 }
import uk.gov.nationalarchives.omega.api.messages.{ MessageProperties, OutgoingMessageType }

import javax.jms.{ Connection, MessageProducer, Session, TextMessage }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ApiServiceISpec
    extends FixtureAsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience
    with BeforeAndAfterEach with BeforeAndAfterAll {

  /** Note: Now that we have multiple scenarios, we see that we cannot run more than one at a time, likely because there
    * is contention with the JMS consumer.
    *
    * As such, the only way for the scenarios to pass (currently) is to run them individuals - and even then you'll
    * regularly see a failure.
    *
    * I think the whole approach to the reply message assertion needs to be improved.
    */

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(30, Seconds)), interval = scaled(Span(1, Seconds)))

  private val requestQueueName = "request-general"
  private val replyQueueName = "omega-editorial-web-application-instance-1"
  private val sqsHostName = "localhost"
  private val sqsPort = 9324

  private val apiService = new ApiService(
    ServiceConfig(
      tempMessageDir = "temp",
      maxConsumers = 1,
      maxProducers = 1,
      maxDispatchers = 1,
      maxLocalQueueSize = 1,
      requestQueue = requestQueueName,
      replyQueue = replyQueueName
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

    "the OMGReplyAddress" - {
      "isn't provided" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(replyAddress = None)

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage(getExpectedJsonErrors(Map(MISS007 -> "Missing OMGReplyAddress"))) *>
          assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)

      }
      "isn't valid" in { f =>
        val textMessageConfig = generateValidMessageConfig().copy(replyAddress = Some("ABCD002."))

        sendMessage(f.session, f.producer, textMessageConfig)

        assertReplyMessage(getExpectedJsonErrors(Map(INVA007 -> "Invalid OMGReplyAddress"))) *>
          assertMessageType(OutgoingMessageType.InvalidMessageFormatError.entryName)

      }
    }
  }

  private def generateValidMessageConfig(): TextMessageConfig =
    TextMessageConfig(
      contents = "Hello, World",
      messageTypeId = Some("OSGESZZZ100"),
      applicationId = Some("ABCD002"),
      messageFormat = Some("application/json"),
      token = Some("AbCdEf123456"),
      replyAddress = Some("ABCD002.a")
    )

  private def asTextMessage(session: Session, messageConfig: TextMessageConfig): TextMessage = {
    val textMessage: TextMessage = session.createTextMessage(messageConfig.contents)
    messageConfig.messageTypeId.foreach { messageTypeId =>
      textMessage.setStringProperty("OMGMessageTypeID", messageTypeId)
    }
    messageConfig.applicationId.foreach { applicationId =>
      textMessage.setStringProperty("OMGApplicationID", applicationId)
    }
    messageConfig.messageFormat.foreach { messageFormat =>
      textMessage.setStringProperty("OMGMessageFormat", messageFormat)
    }
    messageConfig.token.foreach { token =>
      textMessage.setStringProperty("OMGToken", token)
    }
    messageConfig.replyAddress.foreach { replyAddress =>
      textMessage.setStringProperty("OMGReplyAddress", replyAddress)
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

}

case class TextMessageConfig(
  contents: String,
  messageTypeId: Option[String],
  applicationId: Option[String],
  messageFormat: Option[String],
  token: Option[String],
  replyAddress: Option[String]
)
