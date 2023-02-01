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

import cats.effect.{ IO, Resource }
import com.fasterxml.uuid.{ EthernetAddress, Generators }
import jms4s.jms.JmsMessage

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{ Files, Path, StandardOpenOption }
import java.util.UUID

object LocalMessageStore {
  type PersistentMessageId = UUID
}

class LocalMessageStore(folder: Path) {
  private val uuidGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface)

  import LocalMessageStore.PersistentMessageId

  def persistMessage(message: JmsMessage): IO[PersistentMessageId] = {
    def newMessageFileId(): IO[PersistentMessageId] =
      IO.delay {
        uuidGenerator.generate()
      }

    def openNewMessageFile(messageId: PersistentMessageId): IO[ByteChannel] =
      IO.blocking {
        val path = folder.resolve(s"$messageId.msg")
        Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.DSYNC)
      }

    def closeMessageFile(byteChannel: ByteChannel): IO[Unit] =
      IO.blocking {
        byteChannel.close()
      }

    newMessageFileId().flatMap { persistentMessageId: UUID =>
      message
        .asTextF[IO]
        .flatMap { messageText: String =>
          Resource.make(openNewMessageFile(persistentMessageId))(closeMessageFile).use { messageFile =>
            IO.blocking {
              val buffer = ByteBuffer.wrap(messageText.getBytes(UTF_8))
              messageFile.write(buffer)
              persistentMessageId
            }
          }
        }
        .flatTap(uuid => IO.delay(s"Persisted message: $uuid"))
    }
  }
}
