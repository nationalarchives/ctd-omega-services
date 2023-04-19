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

package uk.gov.nationalarchives.omega.api

import org.apache.commons.lang3.SerializationUtils
import org.scalatest.matchers.{ MatchResult, Matcher }
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.services.{ LocalMessage, LocalMessageStore }

import java.nio.file.{ FileSystems, Files, Path, Paths }
import scala.reflect.io.Directory
import scala.util.{ Failure, Success, Try }

trait LocalMessageSupport {

  protected val tempDirectoryPath: Path = Files.createTempDirectory("ApiServicesTesting")
  lazy val localMessageStore = new LocalMessageStore(tempDirectoryPath)

  def generateExpectedFilepath(messageId: Version1UUID): String =
    tempDirectoryPath.toAbsolutePath.toString + "/" + messageId + ".msg"

  def haveACorrespondingFile: Matcher[Version1UUID] = (messageId: Version1UUID) => {
    val filePath = FileSystems.getDefault.getPath(generateExpectedFilepath(messageId))
    MatchResult(
      Files.exists(filePath),
      s"We expected that a file would exist for message ID [$messageId], path [$filePath], but there wasn't one.",
      s"We expected that no file would exist for message ID [$messageId], path [$filePath] but it did."
    )
  }

  def haveFileContents(expectedContents: String): Matcher[Version1UUID] = (messageId: Version1UUID) => {

    def getFileContents(messageId: Version1UUID): Try[LocalMessage] =
      for (
        path    <- Try(Paths.get(generateExpectedFilepath(messageId)));
        bytes   <- Try(Files.readAllBytes(path));
        message <- Try(SerializationUtils.deserialize[LocalMessage](bytes))
      ) yield message

    getFileContents(messageId) match {
      case Success(message) =>
        MatchResult(
          message.messageText.contains(expectedContents),
          s"We expected the file contents for message [$messageId] to be [$expectedContents], but it was [${message.messageText}].",
          s"We didn't expect the file contents for message [$messageId] to be [$expectedContents], but it was."
        )
      case Failure(e) => MatchResult(matches = false, s"Failed to retrieve message contents due to ${e.getMessage}", "")
    }

  }

  def prepareTempDirectory(): Unit =
    tempDirectoryPath.toFile.setWritable(true)

  def deleteTempDirectory(): Unit =
    Directory(tempDirectoryPath.toFile).deleteRecursively()

  def makeTempDirectoryReadOnly(): Unit =
    tempDirectoryPath.toFile.setReadOnly()

}
