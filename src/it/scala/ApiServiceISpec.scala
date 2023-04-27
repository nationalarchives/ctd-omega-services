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
  }

  "The Message API" - {

    "returns an echo message when all fields are valid" in { f =>
      val textMessageConfig = generateValidMessageConfig().copy(contents = "Hello World!")

      sendMessage(f.session, f.producer, textMessageConfig)

      assertReplyMessage("The Echo Service says: Hello World!")

    }

    "returns an error message when" - {
      "the OMGMessageTypeID (aka SID)" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Missing OMGMessageTypeID")

        }
        "is unrecognised" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(messageTypeId = Some("OSGESXXX100"))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Invalid OMGMessageTypeID")

        }
      }
      "the OMGApplicationID" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(applicationId = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Missing OMGApplicationID;Invalid OMGResponseAddress")

        }
        "isn't valid" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(applicationId = Some("ABC001"))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Invalid OMGApplicationID;Invalid OMGResponseAddress")

        }
      }
      "the OMGMessageFormat" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(messageFormat = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Missing OMGMessageFormat")

        }
        "isn't valid" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(messageFormat = Some("text/plain"))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Invalid OMGMessageFormat")

        }
      }
      "the OMGToken" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(token = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Missing OMGToken")

        }
        "isn't valid" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(token = Some(" "))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Invalid OMGToken")

        }
      }
      "the OMGResponseAddress" - {
        "isn't provided" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(responseAddress = None)

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Missing OMGResponseAddress")

        }
        "isn't valid" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(responseAddress = Some("ABCD002."))

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Invalid OMGResponseAddress")

        }
      }
      "the message body is" - {
        "empty (with padding)" in { f =>
          val textMessageConfig = generateValidMessageConfig().copy(contents = " ")

          sendMessage(f.session, f.producer, textMessageConfig)

          assertReplyMessage("Message text is blank: Echo Text cannot be empty.")

        }
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
      responseAddress = Some("ABCD002.a")
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
    messageConfig.responseAddress.foreach { responseAddress =>
      textMessage.setStringProperty("OMGResponseAddress", responseAddress)
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

  private def assertReplyMessage(expectedContents: String): IO[Assertion] =
    eventually {
      replyMessageText.get.asserting(_ mustBe Some(expectedContents))
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
  responseAddress: Option[String]
)
