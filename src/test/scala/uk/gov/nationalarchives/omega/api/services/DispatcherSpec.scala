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
import org.apache.commons.lang3.SerializationUtils
import org.scalatest.TryValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{ Assertion, BeforeAndAfterAll }
import uk.gov.nationalarchives.omega.api.LocalMessageSupport
import uk.gov.nationalarchives.omega.api.business.echo.EchoService
import uk.gov.nationalarchives.omega.api.common.Version1UUID

import java.nio.file.{ FileSystems, Files, NoSuchFileException, StandardOpenOption }
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Try }

class DispatcherSpec
    extends AsyncFreeSpec with BeforeAndAfterAll with AsyncIOSpec with Matchers with TryValues
    with LocalMessageSupport {

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
              generateValidLocalMessageForEchoService().copy(correlationId = None),
              "Missing JMSMessageID"
            )
          }
          "is blank" in {
            assertReplyMessage(
              generateValidLocalMessageForEchoService().copy(correlationId = Some("")),
              "Invalid JMSMessageID"
            )
          }
        }

        "the message" - {
          "is blank" in {
            assertReplyMessage(
              generateValidLocalMessageForEchoService().copy(messageText = ""),
              "Message text is blank: Echo Text cannot be empty."
            )
          }
        }

      }
      "the service" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(serviceId = None),
            "Missing OMGMessageTypeID"
          )
        }
        "isn't valid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(serviceId = Some("ECHO001")),
            "Invalid OMGMessageTypeID"
          )
        }
      }
      "the calling application ID" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(applicationId = None),
            "Missing OMGApplicationID;Invalid OMGResponseAddress"
          )
        }
        "isn't valid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(applicationId = Some("ABC001")),
            "Invalid OMGApplicationID;Invalid OMGResponseAddress"
          )
        }
      }
      "the JMS message timestamp" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(epochTimeInMilliseconds = None),
            "Missing JMSTimestamp"
          )
        }
      }
      "the message format" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(messageFormat = None),
            "Missing OMGMessageFormat"
          )
        }
        "isn't valid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(messageFormat = Some("text/plain")),
            "Invalid OMGMessageFormat"
          )
        }
      }
      "the auth token" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(authToken = None),
            "Missing OMGToken"
          )
        }
        "is invalid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(authToken = Some("")),
            "Invalid OMGToken"
          )
        }
      }
      "the response address" - {
        "isn't provided" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(responseAddress = None),
            "Missing OMGResponseAddress"
          )
        }
        "is invalid" in {
          assertReplyMessage(
            generateValidLocalMessageForEchoService().copy(responseAddress = Some("ABCD002.")),
            "Invalid OMGResponseAddress"
          )
        }
      }
    }

    "for message recovery then it should run recovery and delete all the local messages" in {
      val testMessage = "Testing message recovery!"
      // create a valid message
      // write the message to file in the temporary message store
      writeMessageFile(generateValidLocalMessageForEchoService().copy(messageText = testMessage))
      // read the file from the message store
      val localMessages = localMessageStore.readAllFilesInDirectory().unsafeRunSync()
      // pass the file to runRecovery
      dispatcher.runRecovery(0)(localMessages) *>
        // it should send back the expected reply
        IO(testLocalProducer.message).asserting(_ mustBe s"The Echo Service says: $testMessage") *>
        // you can also check there are no longer any files the local message store
        localMessageStore.readAllFilesInDirectory().asserting(_.length mustBe 0)
    }
  }

  private def assertReplyMessage(localMessage: LocalMessage, expectedReplyMessage: String): Assertion = {

    writeMessageFile(localMessage)

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
    Await.result(io.unsafeToFuture(), 5.second)

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

  /** It is the APIService which is responsible for writing the local message file.
    *
    * However, it is the Dispatcher which is responsible for removing it, once the message has been handled.
    *
    * As such, we need to simulate this.
    */
  private def writeMessageFile(message: LocalMessage): Unit =
    Try(
      Files.write(
        FileSystems.getDefault.getPath(generateExpectedFilepath(message.persistentMessageId)),
        SerializationUtils.serialize(message),
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE,
        StandardOpenOption.DSYNC
      )
    ) match {
      case Success(_) => ()
      case Failure(e) => fail(s"Unable to write the message file for message [${message.persistentMessageId}]: [$e]")
    }

}
