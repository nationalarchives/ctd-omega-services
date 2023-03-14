package fixture

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.scalatest.{ BeforeAndAfterAll, Suite }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig

import java.nio.file.{ Files, Path, Paths }
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Using }

/** API Service Fixture which starts beforeAll tests and stops afterAll tests.
  */
trait ApiServiceAllFixture2 extends BeforeAndAfterAll { this: Suite =>

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  /** Get the path to the directory to use as the Message Directory for the API Service.
    *
    * @return
    *   the path to a directory that will be used for storing messages
    */
  protected def getMessageDir(): Path

  private val apiService: uk.gov.nationalarchives.omega.api.services.ApiService =
    new uk.gov.nationalarchives.omega.api.services.ApiService(
      ServiceConfig("temp", 1, 1, 1, 1, "request-general", "ctd-omega-editorial-web-application-instance-1")
    )

  val replyMessage: String = "reply.json"

  val jmsClient = simpleQueueService.makeJmsClient[IO](
    Config(
      endpoint = simpleQueueService.Endpoint(Some(DirectAddress(HTTP, "localhost", Some(9324))), "elasticmq"),
      credentials = Some(Credentials("x", "x")),
      clientId = simpleQueueService.ClientId("ctd-omega-services"),
      None
    )
  )
  override protected def beforeAll(): Unit = {
    val consumerRes = for {
      client <- jmsClient
      consumer <-
        client.createAutoAcknowledgerConsumer(QueueName("omega-editorial-web-application-instance-1"), 1, 100.millis)
    } yield consumer
    consumerRes
      .use(_.handle { (jmsMessage, _) =>
        for {
          _ <- readTextMessage(jmsMessage)
        } yield AutoAckAction.noOp
      })
      .both(apiService.start) // .unsafeRunAsync() //.unsafeRunSync()
    // this.apiService.start.unsafeRunSync()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          //(ServiceArgs(Some(getMessageDir().toString), Option.empty))
    super.beforeAll() // NOTE(AR) To be stackable, we must call super.beforeAll
  }

  override protected def afterAll(): Unit =
    try
      super.afterAll() // NOTE(AR) To be stackable, we must call super.afterAll
    finally
      this.apiService.stop() // .unsafeRunSync()

  /** Get the API Service.
    *
    * @return
    *   the API Service
    */
  // def getApiService() = this.apiService

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
}
