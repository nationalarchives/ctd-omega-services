import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import fixture.SqsConnector
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.services.ApiService

import java.nio.file.{ Files, Paths }
import javax.jms.{ Connection, MessageProducer, Session }
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.util.{ Failure, Success, Using }

class ApiServiceISpec
    extends AsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience
    with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  private val apiService = new ApiService(
    ServiceConfig("temp", 1, 1, 1, 1, "request-general", "omega-editorial-web-application-instance-1")
  )

  private val replyMessage: String = "reply.json"

  private val jmsClient = simpleQueueService.makeJmsClient[IO](
    Config(
      endpoint = simpleQueueService.Endpoint(Some(DirectAddress(HTTP, "localhost", Some(9324))), "elasticmq"),
      credentials = Some(Credentials("x", "x")),
      clientId = simpleQueueService.ClientId("ctd-omega-services"),
      None
    )
  )
  override def beforeAll(): Unit = {
    val consumerRes = for {
      client <- jmsClient
      consumer <-
        client.createAutoAcknowledgerConsumer(QueueName("omega-editorial-web-application-instance-1"), 1, 100.millis)
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

  override def afterEach(): Unit =
    try
      super.afterEach()
    finally
      if (Files.exists(Paths.get(replyMessage))) {
        Files.delete(Paths.get(replyMessage))
      }

  private def readTextMessage(jmsMessage: JmsMessage): IO[Unit] =
    jmsMessage.asTextF[IO].attempt.map {
      case Left(e) => fail(s"Unable to read message file due to ${e.getMessage}")
      case Right(text) =>
        val file = Paths.get(replyMessage)
        Using(Files.newBufferedWriter(file)) { writer =>
          writer.write(text)
        } match {
          case Success(_) => ()
          case Failure(e) => fail(s"Unable to write message file due to ${e.getMessage}")
        }
    }

  def getFileContent: IO[String] =
    IO.pure(getMessageFromFileStringPath(replyMessage))

  def getMessageFromFileStringPath(messageFilePath: String): String =
    Using(Source.fromFile(messageFilePath)) { source =>
      source.getLines().mkString
    } match {
      case Success(value) => value
      case Failure(_)     => ""
    }

  "The Message API" - {

    "returns an echo message when a text message is sent with a SID of ECHO001" in {
      val sqsTestConnector = SqsConnector("http://localhost:9324")
      val sqsTestConnection: Connection = sqsTestConnector.getConnection
      val session: Session = sqsTestConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
      val searchQueue: String = "request-general"
      val producer: MessageProducer = session.createProducer(session.createQueue(searchQueue))

      val textMessage = session.createTextMessage("Hello World!")
      textMessage.setStringProperty("sid", "ECHO001")
      producer.send(textMessage)
      eventually {
        getFileContent.asserting(_ mustBe "The Echo Service says: Hello World!")
      }
    }

    "returns an error message when the SID is not recognised" in {
      pending // This test is marked pending until completion of https://national-archives.atlassian.net/browse/PACT-836
      val sqsTestConnector = SqsConnector("http://localhost:9324")
      val sqsTestConnection: Connection = sqsTestConnector.getConnection
      val session: Session = sqsTestConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
      val searchQueue: String = "request-general"
      val producer: MessageProducer = session.createProducer(session.createQueue(searchQueue))

      val textMessage = session.createTextMessage("Hello World!")
      textMessage.setStringProperty("sid", "UNKNOWN")
      producer.send(textMessage)
      eventually {
        getFileContent.asserting(_ mustBe "The Echo Service says: Hello World!")
      }
    }

    "returns an error message when the message body is empty" in {
      pending // This test is marked pending until completion of https://national-archives.atlassian.net/browse/PACT-836
      val sqsTestConnector = SqsConnector("http://localhost:9324")
      val sqsTestConnection: Connection = sqsTestConnector.getConnection
      val session: Session = sqsTestConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
      val searchQueue: String = "request-general"
      val producer: MessageProducer = session.createProducer(session.createQueue(searchQueue))

      val textMessage = session.createTextMessage("")
      textMessage.setStringProperty("sid", "ECHO001")
      producer.send(textMessage)
      eventually {
        getFileContent.asserting(_ mustBe "The Echo Service says: Hello World!")
      }
    }
  }

}
