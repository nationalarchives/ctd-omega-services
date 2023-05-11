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
import cats.effect.testing.scalatest.AsyncIOSpec
import jms4s.jms.JmsMessage
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import uk.gov.nationalarchives.omega.api.LocalMessageSupport
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, LocalMessageStore, MessageProperties }

import java.nio.file.{ AccessDeniedException, NoSuchFileException }
import java.time.{ LocalDateTime, ZoneOffset }
import java.util.UUID

class LocalMessageStoreSpec
    extends AsyncFreeSpec with AsyncIOSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach
    with MockitoSugar with LocalMessageSupport {

  override protected def afterAll(): Unit = {
    deleteTempDirectory()
    super.afterAll()
  }

  "The LocalMessageStore when" - {
    "persisting a message" - {
      "when the content is" - {
        "empty" in {

          val mockJmsMessage = generateMockJmsMessage("")

          val messageId = persistMessage(mockJmsMessage)

          messageId.asserting(_ must haveFileContents(""))

          messageId.flatMap(removeMessage).asserting(_ mustBe ())

        }
        "not empty" in {

          val mockJmsMessage = generateMockJmsMessage("Hello, world")

          val messageId = persistMessage(mockJmsMessage)

          messageId.asserting(_ must haveFileContents("Hello, world"))

          messageId.flatMap(removeMessage).asserting(_ mustBe ())

        }

      }
      "when there's a IO failure" in {

        val mockJmsMessage = generateMockJmsMessage("Hello, world")

        makeTempDirectoryReadOnly()

        persistMessage(mockJmsMessage).assertThrows[AccessDeniedException]

      }
    }
    "reading a message" - {
      "when a message has been written" - {
        "it can be read" in {
          val mockJmsMessage = generateMockJmsMessage("Hello, world")

          val messageId = persistMessage(mockJmsMessage)
          val writtenMessage = messageId.flatMap(id => readMessage(id))

          messageId.flatMap(removeMessage).asserting(_ mustBe ())

          writtenMessage.asserting(_.messageText mustEqual "Hello, world")
        }
      }
      "when multiple messages have been written" - {
        "they can be read" in {
          val mockJmsMessage1 = generateMockJmsMessage("Hello, world")
          val mockJmsMessage2 = generateMockJmsMessage("Hello, world, again")

          persistMessage(mockJmsMessage1) *>
            persistMessage(mockJmsMessage2) *>
            // TODO: Messages are not being cleaned up before this test.
            readAllMessages().asserting(_.map(_.messageText).toSet mustEqual Set("Hello, world", "Hello, world, again"))

        }
      }
    }
    "removing a message when" - {
      "the provided ID" - {
        "doesn't have a corresponding file" in {

          val messageId = Version1UUID.generate()
          messageId must not(haveACorrespondingFile)

          removeMessage(messageId).assertThrows[NoSuchFileException]
        }
        "has a corresponding file" in {

          givenThatAFileHasBeenWritten().flatMap { messageId =>
            removeMessage(messageId).asserting(_ mustBe ()) *>
              IO.pure(messageId).asserting(_ must not(haveACorrespondingFile))
          }
        }
      }

      "when there's a IO failure" in {

        val messageId = for {
          id <- givenThatAFileHasBeenWritten()
          _  <- IO.delay(makeTempDirectoryReadOnly()) *> IO.unit
        } yield id

        messageId.flatMap(removeMessage).assertThrows[AccessDeniedException]

        messageId.asserting(_ must haveACorrespondingFile)

      }
      "check if a directory is empty" - {
        "when is is not empty" in {
          val mockJmsMessage = generateMockJmsMessage("")
          val _ = persistMessage(mockJmsMessage)

          LocalMessageStore.checkDirectoryExists(tempDirectoryPath) mustBe true
          LocalMessageStore.checkDirectoryNonEmpty(tempDirectoryPath) mustBe true
        }
      }
      "check if a directory exists" - {
        "when it has been created" in {
          LocalMessageStore.checkDirectoryExists(tempDirectoryPath) mustBe true
        }

        "when it does not exist" in {
          deleteTempDirectory()
          LocalMessageStore.checkDirectoryExists(tempDirectoryPath) mustBe false
        }
      }
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareTempDirectory()
  }

  private def givenThatAFileHasBeenWritten(): IO[Version1UUID] = {
    val contents = "Lorem ipsum dolor sit amet"
    val messageId = for {
      mockJmsMessage <- IO.delay(generateMockJmsMessage(contents))
      id             <- persistMessage(mockJmsMessage)
    } yield id
    messageId.asserting(_ must haveFileContents(contents)) *>
      messageId
  }

  private def persistMessage(mockJmsMessage: JmsMessage): IO[Version1UUID] =
    for {
      tryPersist <- localMessageStore.persistMessage(mockJmsMessage)
      messageId  <- IO.fromTry(tryPersist)
    } yield messageId

  private def readMessage(messageId: Version1UUID): IO[LocalMessage] =
    for {
      tryRead <- localMessageStore.readMessage(messageId)
      message <- IO.fromTry(tryRead)
    } yield message

  private def readAllMessages(): IO[List[LocalMessage]] =
    localMessageStore.readAllFilesInDirectory()

  private def removeMessage(messageId: Version1UUID): IO[Unit] =
    for {
      tryRemove <- localMessageStore.removeMessage(messageId)
      result    <- IO.fromTry(tryRemove)
    } yield result

  private def generateMockJmsMessage(text: String): JmsMessage = {
    val mockJmsMessage = mock[JmsMessage]
    when(mockJmsMessage.asTextF[IO]).thenReturn(IO.pure(text))
    when(mockJmsMessage.getJMSMessageId).thenReturn(Some(UUID.randomUUID().toString))
    when(mockJmsMessage.getJMSTimestamp).thenReturn(Some(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)))
    when(mockJmsMessage.getStringProperty(eqTo(MessageProperties.OMGMessageTypeID))).thenReturn(Some("OSGESZZZ100"))
    when(mockJmsMessage.getStringProperty(eqTo(MessageProperties.OMGApplicationID))).thenReturn(Some("ABCD002"))
    when(mockJmsMessage.getStringProperty(eqTo(MessageProperties.OMGMessageFormat)))
      .thenReturn(Some("application/json"))
    when(mockJmsMessage.getStringProperty(eqTo(MessageProperties.OMGToken))).thenReturn(Some("application"))
    when(mockJmsMessage.getStringProperty(eqTo(MessageProperties.OMGReplyAddress))).thenReturn(Some("ABCD002.a"))
    mockJmsMessage
  }

}
