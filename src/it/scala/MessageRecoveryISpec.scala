import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Ref }
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.apache.commons.lang3.SerializationUtils
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, LocalMessageStore }
import uk.gov.nationalarchives.omega.api.services.ApiService

import java.nio.file._
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Try }

/** This is for testing the Message Recovery function in the ApiService to process the
  * LocalMessage[uk.gov.nationalarchives.omega.api.messages.LocalMessage] files in the LocalMessageStore and sending
  * response messages to the client when the application starts
  */
class MessageRecoveryISpec
    extends AsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience
    with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(5, Millis)))

  private val requestQueueName = "PACS001_request"
  private val defaultReplyQueueName = "PACE001_reply"
  private val sqsHostName = "localhost"
  private val sqsPort = 9324
  private val testMessage = "Testing message recovery!"

  private val messageId: Ref[IO, Option[Version1UUID]] = Ref[IO].of(Option.empty[Version1UUID]).unsafeRunSync()
  private val replyMessageText: Ref[IO, Option[String]] = Ref[IO].of(Option.empty[String]).unsafeRunSync()
  private var tempMsgDir: Option[String] = None
  private var apiService: Option[ApiService] = None

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
        replyMessageText.set(Some(text))
      case Left(e) => IO.delay(fail(s"Unable to read message contents due to ${e.getMessage}"))
    }

  /** Setup resources and services for the test:
    * 1.create a LocalMessage and Serialize the LocalMessage to a temporary directory 2.create an instance of the
    * ApiService 3.set up the consumer to handle messages for the test 4.start the consumer and the ApiService
    */
  override protected def beforeAll(): Unit = {
    val tmpMessage = generateValidLocalMessageForEchoService().copy(messageText = testMessage)
    val path = writeMessageFile(tmpMessage)
    messageId.set(Some(tmpMessage.persistentMessageId)).unsafeRunSync()
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
          sparqlEndpoint = "http://localhost:8080/rdf4j-server/repositories/PACT"
        )
      )
    )

    val consumerRes = for {
      client <- jmsClient
      consumer <-
        client.createAutoAcknowledgerConsumer(QueueName(defaultReplyQueueName), 1, 100.millis)
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

    // TODO(RW) this test is being ignored until PACT-931 and PACT-932 are completed
    "runs the recovery service and removes the message from the message store" ignore {
      val messageStoreFolder = Paths.get(tempMsgDir.get)
      val localMessageStore = new LocalMessageStore(messageStoreFolder)
      eventually {
        messageId.get
          .flatMap(maybeId => localMessageStore.readMessage(maybeId.get))
          .asserting(_.failure.exception mustBe a[NoSuchFileException]) *>
          replyMessageText.get.asserting(_ mustBe Some(s"The Echo Service says: $testMessage"))
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
      omgApplicationId = Some("PACE001"),
      Some(System.currentTimeMillis()),
      Some("application/json"),
      Some(UUID.randomUUID().toString),
      Some("PACE001_reply")
    )

}
