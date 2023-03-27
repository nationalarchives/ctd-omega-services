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
import cats.effect.unsafe.implicits.global
import jms4s.jms.JmsMessage
import org.mockito.MockitoSugar
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import uk.gov.nationalarchives.omega.api.LocalMessageSupport
import uk.gov.nationalarchives.omega.api.common.Version1UUID

import java.nio.file.{ AccessDeniedException, NoSuchFileException }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

class LocalMessageStoreSpec
    extends AnyFreeSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar
    with LocalMessageSupport {

  override protected def afterAll(): Unit = {
    super.afterAll()
    deleteTempDirectory()
  }

  "The LocalMessageStore when" - {
    "persisting a message" - {
      "when the content is" - {
        "empty" in {

          val mockJmsMessage = generateMockJmsMessage("")

          val messageId = persistMessage(mockJmsMessage)

          messageId must haveFileContents("")

        }
        "not empty" in {

          val mockJmsMessage = generateMockJmsMessage("Hello, world")

          val messageId = persistMessage(mockJmsMessage)

          messageId must haveFileContents("Hello, world")

        }

      }
      "when there's a IO failure" in {

        val mockJmsMessage = generateMockJmsMessage("Hello, world")

        makeTempDirectoryReadOnly()

        assertThrows[AccessDeniedException] {
          persistMessage(mockJmsMessage)
        }

      }
    }
    "removing a message when" - {
      "the provided ID" - {
        "is null" in {

          val thrownException = intercept[IllegalArgumentException] {
            removeMessage(null)
          }

          thrownException.getMessage mustBe "A message ID must be provided"

        }
        "doesn't have a corresponding file" in {

          val messageId = Version1UUID.generate()
          messageId must not(haveACorrespondingFile())

          assertThrows[NoSuchFileException] {
            removeMessage(messageId)
          }

        }
        "has a corresponding file" in {

          val messageId = givenThatAFileHasBeenWritten()

          removeMessage(messageId)

          messageId must not(haveACorrespondingFile())

        }
      }

      "when there's a IO failure" in {

        val messageId = givenThatAFileHasBeenWritten()
        makeTempDirectoryReadOnly()

        assertThrows[AccessDeniedException] {
          removeMessage(messageId)
        }

        messageId must haveACorrespondingFile()

      }
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareTempDirectory()
  }

  private def givenThatAFileHasBeenWritten(): Version1UUID = {
    val contents = "Lorem ipsum dolor sit amet"
    val mockJmsMessage = generateMockJmsMessage(contents)
    val messageId = persistMessage(mockJmsMessage)
    messageId must haveFileContents(contents)
    messageId
  }

  private def persistMessage(mockJmsMessage: JmsMessage): Version1UUID =
    await(localMessageStore.persistMessage(mockJmsMessage)) match {
      case Success(messageId) => messageId
      case Failure(e)         => throw e
    }

  private def removeMessage(messageId: Version1UUID): Unit =
    await(localMessageStore.removeMessage(messageId)) match {
      case Success(_) =>
      case Failure(e) => throw e
    }

  private def await[T](io: IO[T]): T =
    Await.result(io.unsafeToFuture(), 1.second)

  private def generateMockJmsMessage(text: String): JmsMessage = {
    val mockJmsMessage = mock[JmsMessage]
    when(mockJmsMessage.asTextF[IO]).thenReturn(IO.pure(text))
    mockJmsMessage
  }

}
