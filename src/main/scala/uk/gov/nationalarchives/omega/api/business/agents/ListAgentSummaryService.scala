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

package uk.gov.nationalarchives.omega.api.business.agents

import cats.data.Validated
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{ InvalidMessagePayload, MessageValidationError, ValidationResult }
import uk.gov.nationalarchives.omega.api.messages.request.{ ListAgentSummary, RequestMessage }
import uk.gov.nationalarchives.omega.api.messages.{ StubData, ValidatedLocalMessage }

import java.text.SimpleDateFormat
import java.util.Date
import scala.util.{ Failure, Success, Try }

class ListAgentSummaryService(val stubData: StubData) extends BusinessService with BusinessRequestValidation {

  override def process(
    requestMessage: RequestMessage
  ): Either[BusinessServiceError, BusinessServiceReply] =
    Try(requestMessage.asInstanceOf[ListAgentSummary]) match {
      case Success(_) =>
        Right(
          ListAgentSummaryReply(
            stubData
              .getAgentSummaries()
              .asJson
              .toString()
          )
        )
      case Failure(exception) => Left(ListAgentSummaryError(exception.getMessage))
    }

  override def validateRequest(validatedLocalMessage: ValidatedLocalMessage): ValidationResult[RequestMessage] =
    if (validatedLocalMessage.messageText.nonEmpty) {
      decode[ListAgentSummary](validatedLocalMessage.messageText) match {
        case Right(request) =>
          val versionIdentifiers: List[String] = List("latest", "all")
          if (request.versionTimestamp.isEmpty || versionIdentifiers.contains(request.versionTimestamp.get))
            Validated.valid(request)
          else {
            validateDate(request.versionTimestamp.get) match {
              case Some(_) =>
                Validated.valid(request)
              case _ =>
                Validated.invalidNec[MessageValidationError, RequestMessage](
                  InvalidMessagePayload(Some(s"Invalid date: ${request.versionTimestamp.get}"))
                )
            }
          }
        case Left(error) =>
          Validated.invalidNec[MessageValidationError, RequestMessage](
            InvalidMessagePayload(cause = Some(error))
          )
      }

    } else {
      Validated.valid(ListAgentSummary(List.empty))
    }

  private def validateDate(dateStr: String): Option[Date] = {

    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    val trimmedDate = dateStr.trim
    if (trimmedDate.isEmpty) None
    else
      Try(dateFormatter.parse(trimmedDate)).toOption
  }

}
