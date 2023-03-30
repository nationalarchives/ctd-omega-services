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
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.LocalMessageSupport
import uk.gov.nationalarchives.omega.api.business.echo.EchoService
import uk.gov.nationalarchives.omega.api.common.Version1UUID

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{ FileSystems, Files, StandardOpenOption }
import java.util.UUID
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

  "When the dispatcher receives a message" - {

    "for the ECHO001 service it should reply with an echo message" in {
      Queue.bounded[IO, LocalMessage](1).flatMap { queue =>
        val messageId = Version1UUID.generate()
        val contents = "Hello World!"
        writeMessageFile(messageId, contents)
        queue
          .offer(
            LocalMessage(
              messageId,
              contents,
              Some(ServiceIdentifier.ECHO001),
              Some(s"ID:${UUID.randomUUID()}")
            )
          ) *>
          dispatcher.run(1)(queue).andWait(5.seconds) *>
          IO(testLocalProducer.message).asserting(_ mustBe "The Echo Service says: Hello World!") *>
          queue.size.asserting(_ mustBe 0) *>
          IO.pure(messageId).asserting(_ must not(haveACorrespondingFile()))
      }
    }

    "for an unknown service it should reply with an error message" in {
      pending // Until completion of https://national-archives.atlassian.net/browse/PACT-836
    }

    "without a message ID it should reply with an error message" in {
      pending // Until completion of https://national-archives.atlassian.net/browse/PACT-836
    }

    "without any text it should reply with an error message" in {
      pending // Until completion of https://national-archives.atlassian.net/browse/PACT-836
    }
  }

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

}
