import TestConstants._
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Ref }
import jms4s.JmsAutoAcknowledgerConsumer
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP, HTTPS }
import org.apache.commons.lang3.SerializationUtils
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach, FutureOutcome }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, LocalMessageStore }
import uk.gov.nationalarchives.omega.api.services.ApiService

import java.nio.file._
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Try }

/** This is for testing the Message Recovery function in the ApiService to process the
  * LocalMessage[uk.gov.nationalarchives.omega.api.messages.LocalMessage] files in the LocalMessageStore and sending
  * response messages to the client when the application starts
  */
class MessageRecoveryISpec
    extends FixtureAsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience
    with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(5, Millis)))

  private val testMessage = "Testing message recovery!"

  private val messageId: Ref[IO, Option[Version1UUID]] = Ref[IO].of(Option.empty[Version1UUID]).unsafeRunSync()
  private val replyMessageText: Ref[IO, Option[String]] = Ref[IO].of(Option.empty[String]).unsafeRunSync()
  private var tempMsgDir: Option[String] = None

  private val sqsProtocol = sqsTls match {
    case true => HTTPS
    case false => HTTP
  }

  private val jmsClient = simpleQueueService.makeJmsClient[IO](
    Config(
      "elasticmq",
      endpoint = Some(simpleQueueService.Endpoint(Some(DirectAddress(sqsProtocol, sqsHost, Some(sqsPort))), Some(Credentials("x", "x")))),
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

  case class MessageRecoveryFixture(consumerRes: Resource[IO, JmsAutoAcknowledgerConsumer[IO]])

  override type FixtureParam = MessageRecoveryFixture
  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
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
    super.withFixture(test.toNoArgAsyncTest(MessageRecoveryFixture(consumerRes)))
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
    ()
  }

  "The Message Recovery API" - {

    "runs the recovery service and removes the message from the message store" in { f =>
      val messageStoreFolder = Paths.get(tempMsgDir.get)
      val localMessageStore = new LocalMessageStore(messageStoreFolder)
      val apiService = new ApiService(TestServiceConfig(messageStoreDir = tempMsgDir.get))
      val serviceIO = apiService.startSuspended
      val res = for {
        consumerFiber   <- f.consumerRes.start
        apiServiceFiber <- Resource.liftK(serviceIO.start)
        result          <- Resource.liftK(assertRecoveredMessage(localMessageStore))
        _               <- Resource.eval(apiServiceFiber.cancel)
        _               <- consumerFiber.cancel
      } yield result
      res.use(assert => IO.pure(assert))
    }
  }

  private def assertRecoveredMessage(localMessageStore: LocalMessageStore): IO[Assertion] =
    eventually {
      messageId.get
        .flatMap(maybeId => localMessageStore.readMessage(maybeId.get))
        .asserting(_.failure.exception mustBe a[NoSuchFileException]) *>
        replyMessageText.get.asserting(_ mustBe Some(s"The Echo Service says: $testMessage"))
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
      Some("PACE001_REPLY001")
    )

}
