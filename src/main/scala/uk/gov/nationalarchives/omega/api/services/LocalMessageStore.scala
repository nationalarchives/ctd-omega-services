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
import cats.syntax.all._
import jms4s.jms.JmsMessage
import org.apache.commons.lang3.SerializationUtils
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.common.Version1UUID

import java.nio.file.{ Files, Path, StandardOpenOption }
import scala.util.{ Failure, Success, Try }

class LocalMessageStore(directoryPath: Path) {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  def persistMessage(jmsMessage: JmsMessage): IO[Try[Version1UUID]] = {
    val messageId = Version1UUID.generate()
    val localMessage = LocalMessage.createLocalMessage(messageId, jmsMessage)
    writeFile(generateFilePath(messageId), localMessage).map {
      case Success(_) =>
        logger.info(s"Successfully persisted the message for ID [$messageId]")
        Success(messageId)
      case Failure(e) =>
        logger.error(s"Failed to persist the message for ID [$messageId]")
        Failure[Version1UUID](e)
    }
  }

  def readMessage(messageId: Version1UUID): IO[Try[LocalMessage]] =
    deserializeFile(
      generateFilePath(messageId)
    )

  def readAllFilesInDirectory(): IO[List[LocalMessage]] =
    directoryPath.toFile().listFiles().toList.traverse { path =>
      deserializeFile(path.toPath()).map(_.toOption)
    }.map(_.flatten)

  def removeMessage(messageId: Version1UUID): IO[Try[Unit]] =
    removeFile(generateFilePath(messageId))

  private def deserializeFile(path: Path): IO[Try[LocalMessage]] =
    IO.blocking {
      Try {
        SerializationUtils.deserialize[LocalMessage](
          Files.readAllBytes(path)
        )
      }
    }

  private def generateFilePath(messageId: Version1UUID): Path =
    directoryPath.resolve(messageId.toString + ".msg")

  private def writeFile(path: Path, localMessage: IO[LocalMessage]): IO[Try[Unit]] =
    localMessage.map { m =>
      Try(
        Files.write(
          path,
          SerializationUtils.serialize(m),
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE,
          StandardOpenOption.DSYNC
        )
      )
    }

  private def removeFile(path: Path): IO[Try[Unit]] =
    IO.blocking {
      Try {
        Files.delete(path)
      }
    }

}

object LocalMessageStore {

  def checkDirectoryExists(path: Path): Boolean =
    Files.exists(path) && Files.isDirectory(path)

  def checkDirectoryNonEmpty(path: Path): Boolean =
    checkDirectoryExists(path) && Files.list(path).findAny().isPresent()

}
