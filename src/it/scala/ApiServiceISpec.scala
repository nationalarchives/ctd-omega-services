import cats.effect.IO
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
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FutureOutcome }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.services.ApiService

import javax.jms.{ Connection, MessageProducer, Session }
import scala.concurrent.duration.DurationInt

class ApiServiceISpec
    extends FixtureAsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience
    with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(5, Millis)))

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

  var replyMessageText: Option[String] = None
  var replyMessageId: Option[String] = None

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
    apiService.stop().unsafeToFuture()

  override def afterEach(): Unit = replyMessageText = None

  private def readTextMessage(jmsMessage: JmsMessage): IO[Unit] = {
    replyMessageId = jmsMessage.getJMSCorrelationId
    jmsMessage.asTextF[IO].attempt.map {
      case Right(text) =>
        replyMessageText = Some(text)
      case Left(e) => fail(s"Unable to read message file due to ${e.getMessage}")
    }
  }

  "The Message API" - {

    "returns an echo message when a text message is sent with a SID of ECHO001" in { f =>
      val textMessage = f.session.createTextMessage("Hello World!")
      textMessage.setStringProperty("sid", "ECHO001")
      f.producer.send(textMessage)
      eventually {
        IO.pure(replyMessageText).asserting(_ mustBe Some("The Echo Service says: Hello World!"))
      }
    }

    "returns an error message when the SID is not recognised" in { _ =>
      pending // This test is marked pending until completion of https://national-archives.atlassian.net/browse/PACT-836
    }

    "returns an error message when the message body is empty" in { _ =>
      pending // This test is marked pending until completion of https://national-archives.atlassian.net/browse/PACT-836
    }
  }

}
