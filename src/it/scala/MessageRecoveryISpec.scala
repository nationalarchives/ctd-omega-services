import cats.effect.{ IO, Ref }
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.apache.commons.lang3.SerializationUtils
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
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
import java.util.UUID
import scala.concurrent.Await
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
  private val testMessage = "Testing message recovery!"

  private var messageId: Option[Version1UUID] = None

  var replyMessageText: IO[Ref[IO, Option[String]]] = Ref[IO].of(Option.empty[String])
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

  private def readTextMessage(jmsMessage: JmsMessage): IO[Unit] =
    jmsMessage.asTextF[IO].attempt.flatMap {
      case Right(text) =>
        replyMessageText.map(_.set(Some(text)))
      case Left(e) => IO.delay(fail(s"Unable to read message contents due to ${e.getMessage}"))
    }

  override protected def beforeAll(): Unit = {
    // load messages to disk
    // create a valid message
    val tmpMessage = generateValidLocalMessageForEchoService().copy(messageText = testMessage)
    // write the message to file in the temporary message store
    val path = writeMessageFile(tmpMessage)
    messageId = Some(tmpMessage.persistentMessageId)
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
    Await.result(apiService.get.stop().unsafeToFuture(), 1.minute)

  "The Message Recovery API" - {

    "runs the recovery service and removes the message from the message store" in {
      val messageStoreFolder = Paths.get(tempMsgDir.get)
      val localMessageStore = new LocalMessageStore(messageStoreFolder)
      eventually {
        localMessageStore.readMessage(messageId.get).asserting(_.failure.exception mustBe a[NoSuchFileException]) *>
          replyMessageText.flatMap(_.get.asserting(_ mustBe Some(s"The Echo Service says: $testMessage")))
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

  private def generateValidLocalMessageForEchoService(): LocalMessage =
    LocalMessage(
      Version1UUID.generate(),
      "Hello World!",
      Some("OSGESZZZ100"),
      Some(UUID.randomUUID().toString),
      applicationId = Some("ABCD002"),
      Some(System.currentTimeMillis()),
      Some("application/json"),
      Some(UUID.randomUUID().toString),
      Some("ABCD002.a")
    )

}
