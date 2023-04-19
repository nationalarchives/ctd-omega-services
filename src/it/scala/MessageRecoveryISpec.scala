import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.apache.commons.lang3.SerializationUtils
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FutureOutcome }
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.services.{ ApiService, LocalMessage, LocalMessageStore }

import java.nio.file.{ FileSystems, Files, NoSuchFileException, Path, Paths, StandardOpenOption }
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Try }

class MessageRecoveryISpec
    extends AsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience
    with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(5, Millis)))

  private val requestQueueName = "request-general"
  private val replyQueueName = "omega-editorial-web-application-instance-1"
  private val sqsHostName = "localhost"
  private val sqsPort = 9324

  private val messageId = Version1UUID.generate()

  var replyMessageText: Option[String] = None
  var replyMessageId: Option[String] = None
  var tempMsgDir: Option[String] = None
  var apiService: Option[ApiService] = None

  private val jmsClient = simpleQueueService.makeJmsClient[IO](
    Config(
      endpoint = simpleQueueService.Endpoint(Some(DirectAddress(HTTP, sqsHostName, Some(sqsPort))), "elasticmq"),
      credentials = Some(Credentials("x", "x")),
      clientId = simpleQueueService.ClientId("ctd-omega-services"),
      None
    )
  )

  private def readTextMessage(jmsMessage: JmsMessage): IO[Unit] = {
    replyMessageId = jmsMessage.getJMSCorrelationId
    jmsMessage.asTextF[IO].attempt.map {
      case Right(text) =>
        replyMessageText = Some(text)
      case Left(e) => fail(s"Unable to read message contents due to ${e.getMessage}")
    }
  }

  override protected def beforeAll(): Unit = {
    // load messages to disk
    val path = writeMessageFile(new LocalMessage(messageId, "Test World!", None, None))
    tempMsgDir = Some(path.getParent.toString)
    apiService = Some(
      new ApiService(
        ServiceConfig(
          tempMessageDir = tempMsgDir.get,
          maxConsumers = 1,
          maxProducers = 1,
          maxDispatchers = 1,
          maxLocalQueueSize = 1,
          requestQueue = requestQueueName,
          replyQueue = replyQueueName
        )
      )
    )

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
    apiService.get.start.unsafeToFuture()
    ()
  }
  override def afterAll(): Unit =
    apiService.get.stop().unsafeToFuture()

  "The Message Recovery API" - {

    "runs the recovery service and removes the message from the message store" in {
      val messageStoreFolder = Paths.get(apiService.get.config.tempMessageDir)
      val localMessageStore = new LocalMessageStore(messageStoreFolder)
      eventually {
        localMessageStore.readMessage(messageId).asserting(_.failure.exception mustBe a[NoSuchFileException]) *>
          IO.pure(replyMessageText).asserting(_ mustBe Some("The Echo Service says: Test World!"))
      }
    }

  }

  private def writeMessageFile(message: LocalMessage): Path =
    Try(
      Files.write(
        FileSystems.getDefault.getPath(generateExpectedFilepath(message.persistentMessageId)),
        SerializationUtils.serialize(message),
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE,
        StandardOpenOption.DSYNC
      )
    ) match {
      case Success(path) => path
      case Failure(e)    => fail(s"Unable to write the message file for message [${message.persistentMessageId}]: [$e]")
    }

  def generateExpectedFilepath(messageId: Version1UUID): String =
    Files.createTempDirectory("temp").toAbsolutePath.toString + "/" + messageId + ".msg"

}
