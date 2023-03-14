import cats.effect.IO
import fixture.{ ApiServiceAllFixture, SqsConnector, TemporaryDirectoryAllFixture }
import io.circe.Json
import io.circe.parser.parse
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }

import java.net.URL
import java.nio.file.{ Files, Path, Paths }
import javax.jms.{ Connection, MessageProducer, Session }
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.util.{ Failure, Success, Using }

class ApiServiceISpec
    extends AnyWordSpec with Matchers with Eventually with IntegrationPatience with BeforeAndAfterEach
    with BeforeAndAfterAll with ApiServiceAllFixture with TemporaryDirectoryAllFixture {

  override protected def getMessageDir(): Path =
    getTemporaryDirectory()

  // This configuration sets the timeout times for integration tests to avoid them getting stuck
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(5, Millis)))

  private val replyMessage: String = "reply.json"

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  "The Message API" must {

    "return an error 1001 when the message has an invalid JMS message type" in new MessageTest {
      val jmsClient = simpleQueueService.makeJmsClient[IO](
        Config(
          endpoint = simpleQueueService.Endpoint(Some(DirectAddress(HTTP, "localhost", Some(9324))), "elasticmq"),
          credentials = Some(Credentials("x", "x")),
          clientId = simpleQueueService.ClientId("ctd-omega-services"),
          None
        )
      )
      val consumerRes = for {
        client <- jmsClient
        consumer <-
          client.createAutoAcknowledgerConsumer(QueueName("omega-editorial-web-application-instance-1"), 1, 100.millis)
      } yield consumer
      consumerRes.use(_.handle { (jmsMessage, _) =>
        for {
          _ <- readTextMessage(jmsMessage)
        } yield AutoAckAction.noOp
      })
      private val bytesMessage = session.createBytesMessage()
      bytesMessage.writeBytes("Hello World!".getBytes)
      producer.send(bytesMessage)
      eventually {
        val jsonResultText = getMessageFromFileStringPath(replyMessage)
        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
        val cursor = jsonResultDoc.hcursor
        cursor.downField("errorCode").as[Int].getOrElse(0) mustBe 1001
      }
    }

    "return an error 1002 when the message content is bad" ignore new MessageTest {
      /*
       * Although this error is theoretically possible it is only triggered when the getText method of the JMS
       * TextMessage implementation throws a JMSException. In the current Amazon SQS implementation, SQSTextMessage, no
       * such exception can be thrown. A unit test also exists for this error where it is possible to mimic the throwing
       * of an exception.
       */
    }

//    "return an error 1003 when the message is empty" in new MessageTest {
//      private val textMessage = session.createTextMessage(" ")
//      producer.send(textMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("errorCode").as[Int].getOrElse(0) mustBe 1003
//      }
//    }

//    "return an error 1004 when the message JSON in invalid" in new MessageTest {
//      private val invalidMessageFilePath = getClass.getClassLoader.getResource("message-invalid.json")
//      private val messageContent = getMessageFromFile(invalidMessageFilePath)
//      private val invalidMessageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(invalidMessageJson.toString())
//      producer.send(jsonMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("errorCode").as[Int].getOrElse(0) mustBe 1004
//      }
//    }

//    "return an error 1004 when the message has an invalid search type" in new MessageTest {
//      private val invalidMessageFilePath = getClass.getClassLoader.getResource("message-invalid-searchtype.json")
//      private val messageContent = getMessageFromFile(invalidMessageFilePath)
//      private val invalidMessageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(invalidMessageJson.toString())
//      producer.send(jsonMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("errorCode").as[Int].getOrElse(0) mustBe 1004
//      }
//    }

//    "return an empty result with a matching correlation ID when no results found" in new MessageTest {
//      private val messageFilePath = getClass.getClassLoader.getResource("message-not-found.json")
//      private val messageContent = getMessageFromFile(messageFilePath)
//      private val messageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(messageJson.toString())
//      producer.send(jsonMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("correlationId").as[String].getOrElse("") mustBe "123"
//      }
//    }

//    "return a result with a matching correlation ID when all OK" in new MessageTest {
//      private val messageFilePath = getClass.getClassLoader.getResource("message.json")
//      private val messageContent = getMessageFromFile(messageFilePath)
//      private val messageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(messageJson.toString())
//      producer.send(jsonMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("correlationId").as[String].getOrElse("") mustBe "123"
//      }
//    }

//    "return an out-of-sync error" in new MessageTest {
//      private val messageFilePath = getClass.getClassLoader.getResource("message-targeting-out-of-sync-data.json")
//      private val messageContent = getMessageFromFile(messageFilePath)
//      private val messageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(messageJson.toString())
//      producer.send(jsonMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("errorCode").as[Int].getOrElse(0) mustBe 4012
//      }
//    }

//    "return a result with a matching correlation ID when all OK with default application configuration" in new MessageTest {
//      private val messageFilePath = getClass.getClassLoader.getResource("message.json")
//      private val messageContent = getMessageFromFile(messageFilePath)
//      private val messageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(messageJson.toString())
//      producer.send(jsonMessage)
//      eventually {
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("correlationId").as[String].getOrElse("") mustBe "123"
//      }
//    }

//    // TODO: This test covers the situation when an invalid elastic search endpoint is provided in config.
//    // Currently, the application does not complain about the endpoint being invalid, it will accept and create ES
//    // requests, but given the endpoint is incorrect, the ES client will just sit down and wait for the response to come
//    // back. I would be expected that upon startup, the application should be able to discern if ES is available in the
//    // configured endpoint and stop the startup process if ES is not available or incorrectly configured.
//    "error when provided with an invalid application configuration" ignore new MessageTest {
//      private val invalidConfigFilePath = Option("src/it/config/invalid-config.conf")
//      private val messageFilePath = getClass.getClassLoader.getResource("message.json")
//      private val messageContent = getMessageFromFile(messageFilePath)
//      private val messageJson = getJson(messageContent)
//      private val jsonMessage = session.createTextMessage(messageJson.toString())
//      producer.send(jsonMessage)
//      val directory: Path = Directory.createTemporaryDirectory()
//      val apiService: ApiService = new ApiService()
//      try {
//        apiService.start(ServiceArgs(Some(directory.toString), invalidConfigFilePath))
//        eventually(Files.exists(Paths.get(replyMessage)) mustBe true)
//        val jsonResultText = getMessageFromFileStringPath(replyMessage)
//        val jsonResultDoc = parse(jsonResultText).getOrElse(Json.Null)
//        print("Error trace")
//        print(jsonResultText)
//        val cursor = jsonResultDoc.hcursor
//        cursor.downField("errorCode").as[Int].getOrElse(0) mustBe 3001
//      } finally {
//        apiService.stop()
//        Directory.deleteDirectory(directory)
//      }
//    }
  }

  override def afterEach(): Unit =
    try
      super.afterEach()
    finally
      Files.delete(Paths.get(replyMessage))

  override protected def beforeAll(): Unit =
    // BulkLoadDataIT.createFullTextIndex()
    // BulkLoadDataIT.createTriplestore()
    super.beforeAll()

  trait MessageTest {
    private val sqsTestConnector = SqsConnector("http://localhost:9324")
    val sqsTestConnection: Connection = sqsTestConnector.getConnection
    // val testProducer: ApiProducer = ApiProducer(sqsTestConnection)
    // val testListener: TestListener = TestListener(replyMessage)
    val searchQueue: String = "request-general"
    val searchReplyQueue = "search-reply-queue"
    val searchErrorQueue = "search-error-queue"
    val configFilePath: Option[String] = Option.empty
    // val errorQueueConsumer: ApiConsumer = ApiConsumer(sqsTestConnection, testListener)
    // errorQueueConsumer.listen(searchErrorQueue)
    // val replyQueueConsumer: ApiConsumer = ApiConsumer(sqsTestConnection, testListener)
    // replyQueueConsumer.listen(searchReplyQueue)
    val session: Session = sqsTestConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val producer: MessageProducer = session.createProducer(session.createQueue(searchQueue))

//    def sendMessage(queueName: String, messageJson: Json, correlationId: Option[String]): Assertion =
//      testProducer.sendMessage(queueName, messageJson, correlationId) match {
//        case Right(messageId) =>
//          messageId must fullyMatch regex "^ID:[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$"
//        case _ =>
//          println("Failed to send message")
//          fail()
//      }

    def getMessageFromFile(messageFilePath: URL): String =
      Using(Source.fromURL(messageFilePath)) { source =>
        source.getLines().mkString
      } match {
        case Success(value) => value
        case Failure(_)     => ""
      }

    def getMessageFromFileStringPath(messageFilePath: String): String =
      Using(Source.fromFile(messageFilePath)) { source =>
        source.getLines().mkString
      } match {
        case Success(value) => value
        case Failure(_)     => ""
      }

    def getJson(jsonText: String): Json =
      parse(jsonText) match {
        case Right(json) => json
        case Left(_)     => Json.Null
      }
  }

//  private def createMessageHandler(jmsAcknowledgerConsumer: JmsAutoAcknowledgerConsumer[IO]): IO[Unit] =
//    jmsAcknowledgerConsumer.handle { (jmsMessage, _) =>
//      for {
//        _ <- readTextMessage(jmsMessage)
//      } yield AutoAckAction.noOp
//    }

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
