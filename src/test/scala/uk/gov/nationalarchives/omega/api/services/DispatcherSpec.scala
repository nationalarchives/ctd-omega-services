/*
 * Copyright (c) 2023 The National Archives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.gov.nationalarchives.omega.api.services

import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.testing.scalatest.AsyncIOSpec
import jms4s.config.QueueName
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{ Assertion, BeforeAndAfterAll }
import uk.gov.nationalarchives.omega.api.LocalMessageSupport
import uk.gov.nationalarchives.omega.api.business.echo.EchoService
import uk.gov.nationalarchives.omega.api.common.{ ErrorCode, Version1UUID }
import io.circe._
import io.circe.parser._
import org.scalatest.concurrent.IntegrationPatience
import uk.gov.nationalarchives.omega.api.common.ErrorCode.{ BLAN001, INVA001, INVA002, INVA003, INVA005, INVA006, INVA007, MISS001, MISS002, MISS003, MISS004, MISS005, MISS006, MISS007 }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{ FileSystems, Files, StandardOpenOption }
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Try }

class DispatcherSpec
    extends AsyncFreeSpec with BeforeAndAfterAll with AsyncIOSpec with Matchers with LocalMessageSupport {

  private val testQueue = QueueName("test-queue")
  private val testLocalProducer = new TestProducerImpl(testQueue)
  private val echoService = new EchoService()
  private lazy val dispatcher = new Dispatcher(testLocalProducer, localMessageStore, echoService)

  override protected def afterAll(): Unit = {
    super.afterAll()
    deleteTempDirectory()
  }

  "When the dispatcher receives a message when" - {

    "it's for the ECHO001 service" - {

      "and all fields are provided and valid" in {
        assertReplyMessage(generateValidLocalMessageForEchoService(), "The Echo Service says: Hello World!")
      }
      "a single field is either absent or invalid, namely" - {
        "the JMS message ID (aka correlation ID)" - {
          "isn't provided" in {
            assertReplyMessage(
              generateValidLocalMessageForEchoService().copy(jmsMessageId = None),
              getExpectedJsonErrors(Map(MISS001 -> "Missing JMSMessageID"))
            )
          }
          "is blank" in {
            assertReplyMessage(
              generateValidLocalMessageForEchoService().copy(jmsMessageId = Some("")),
              getExpectedJsonErrors(Map(INVA001 -> "Invalid JMSMessageID"))
            )
          }
        }

        "the message" - {
          "is blank" in {
            assertReplyMessage(
              generateValidLocalMessageForEchoService().copy(messageText = ""),
              getExpectedJsonErrors(Map(BLAN001 -> "Message text is blank: Echo Text cannot be empty."))
            )
          }
        }

      }
      "the service" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgMessageTypeId = None),
            getExpectedJsonErrors(Map(MISS002 -> "Missing OMGMessageTypeID"))
          )
        }
        "isn't valid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgMessageTypeId = Some("ECHO001")),
            getExpectedJsonErrors(Map(INVA002 -> "Invalid OMGMessageTypeID"))
          )
        }
      }
      "the calling application ID" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgApplicationId = None),
            getExpectedJsonErrors(Map(MISS003 -> "Missing OMGApplicationID"))
          )
        }
        "isn't valid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgApplicationId = Some("ABC001")),
            getExpectedJsonErrors(Map(INVA003 -> "Invalid OMGApplicationID"))
          )
        }
      }
      "the JMS message timestamp" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(jmsTimestamp = None),
            getExpectedJsonErrors(Map(MISS004 -> "Missing JMSTimestamp"))
          )
        }
      }
      "the message format" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgMessageFormat = None),
            getExpectedJsonErrors(Map(MISS005 -> "Missing OMGMessageFormat"))
          )
        }
        "isn't valid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgMessageFormat = Some("text/plain")),
            getExpectedJsonErrors(Map(INVA005 -> "Invalid OMGMessageFormat"))
          )
        }
      }
      "the auth token" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgToken = None),
            getExpectedJsonErrors(Map(MISS006 -> "Missing OMGToken"))
          )
        }
        "is invalid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgToken = Some("")),
            getExpectedJsonErrors(Map(INVA006 -> "Invalid OMGToken"))
          )
        }
      }
      "the reply address" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgReplyAddress = None),
            getExpectedJsonErrors(Map(MISS007 -> "Missing OMGReplyAddress"))
          )
        }
        "is invalid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(omgReplyAddress = Some("ABCD002.")),
            getExpectedJsonErrors(Map(INVA007 -> "Invalid OMGReplyAddress"))
          )
        }
      }
    }
  }

  private def assertReplyMessage(localMessage: LocalMessage, expectedReplyMessage: String): Assertion = {

    writeMessageFile(localMessage.persistentMessageId, localMessage.messageText)

    await {
      Queue.bounded[IO, LocalMessage](1).flatMap { queue =>
        queue.offer(localMessage) *>
          dispatcher.run(1)(queue).andWait(1.seconds) *>
          IO(testLocalProducer.message).asserting(_ mustBe expectedReplyMessage) *>
          queue.size.asserting(_ mustBe 0) *>
          IO.pure(localMessage.persistentMessageId).asserting(_ must not(haveACorrespondingFile))
      }
    }
  }

  private def await[T](io: IO[T]): T =
    Await.result(io.unsafeToFuture(), 60.second)

  private def generateValidLocalMessageForEchoService(): LocalMessage =
    LocalMessage(
      Version1UUID.generate(),
      "Hello World!",
      Some("OSGESZZZ100"),
      Some(UUID.randomUUID().toString),
      omgApplicationId = Some("ABCD002"),
      Some(System.currentTimeMillis()),
      Some("application/json"),
      Some(UUID.randomUUID().toString),
      Some("ABCD002.a")
    )

  /** It is the APIService which is responsible for writing the local message file.
    *
    * However, it is the Dispatcher which is responsible for removing it, once the message has been handled.
    *
    * As such, we need to simulate this.
    */
  private def writeMessageFile(messageId: Version1UUID, messageText: String): Unit =
    Try(
      Files.write(
        FileSystems.getDefault.getPath(generateExpectedFilepath(messageId)),
        messageText.getBytes(UTF_8),
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE,
        StandardOpenOption.DSYNC
      )
    ) match {
      case Success(_) => ()
      case Failure(e) => fail(s"Unable to write the message file for message [$messageId]: [$e]")
    }

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

}
