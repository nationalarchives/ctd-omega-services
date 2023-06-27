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
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidation, BusinessRequestValidationError, BusinessService, BusinessServiceError, BusinessServiceReply, BusinessServiceRequest, InvalidAgentSummaryRequestError }
import uk.gov.nationalarchives.omega.api.messages.StubData
import uk.gov.nationalarchives.omega.api.models.ListAgentSummary

import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Try

class ListAgentSummaryService(val stubData: StubData) extends BusinessService with BusinessRequestValidation {

  override def process(
    businessServiceRequest: BusinessServiceRequest
  ): Either[BusinessServiceError, BusinessServiceReply] =
    decode[ListAgentSummary](businessServiceRequest.text.getOrElse(defaultRequest)) match {
      case Left(e) =>
        Left(AgentSummaryRequestError(s"There was an error reading the message: ${e.getMessage}"))
      case Right(_) =>
        Right(
          ListAgentSummaryReply(
            stubData
              .getAgentSummaries()
              .asJson
              .toString()
          )
        )

    }

  override def validateRequest(businessServiceRequest: BusinessServiceRequest): ValidationResult =
    if (businessServiceRequest.text.nonEmpty) {
      decode[ListAgentSummary](businessServiceRequest.text.get) match {
        case Right(request) =>
          val versionIdentifiers: List[String] = List("latest", "all")
          if (request.versionTimestamp.isEmpty || versionIdentifiers.contains(request.versionTimestamp.get))
            Validated.valid(ListAgentSummaryRequest(businessServiceRequest.text, Some(request)))
          else {
            validateDate(request.versionTimestamp.get) match {
              case Some(_) =>
                Validated.valid(ListAgentSummaryRequest(businessServiceRequest.text, Some(request)))
              case _ =>
                Validated.invalidNec[BusinessRequestValidationError, BusinessServiceRequest](
                  InvalidAgentSummaryRequestError(s"Error parsing invalid input date")
                )
            }
          }
        case Left(_) =>
          Validated.invalidNec[BusinessRequestValidationError, BusinessServiceRequest](
            InvalidAgentSummaryRequestError(
              s"Error decoding request message"
            )
          )
      }

    } else {
      Validated.valid(businessServiceRequest)
    }

  private def validateDate(dateStr: String): Option[Date] = {

    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    val trimmedDate = dateStr.trim
    if (trimmedDate.isEmpty) None
    else
      Try(dateFormatter.parse(trimmedDate)).toOption
  }

  private def defaultRequest: String = {
    val defaultRequest = new StringBuilder(s"""{
                                              |    "type" : ["Corporate Body","Person"],
                                              |    "authority-file" : false,
                                              |    "depository" : false,
                                              |    "version-timestamp" : "latest"
                                              |}""".stripMargin)
    defaultRequest.mkString
  }

}
