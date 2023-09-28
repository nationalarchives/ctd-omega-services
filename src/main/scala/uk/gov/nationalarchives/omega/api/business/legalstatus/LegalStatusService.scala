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

package uk.gov.nationalarchives.omega.api.business.legalstatus

import cats.data.Validated
import io.circe.syntax.EncoderOps
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidation, BusinessService, BusinessServiceError, BusinessServiceReply }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.ValidationResult
import uk.gov.nationalarchives.omega.api.messages.ValidatedLocalMessage
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatusSummary
import uk.gov.nationalarchives.omega.api.messages.request.{ ListAssetLegalStatusSummary, RequestMessage }
import uk.gov.nationalarchives.omega.api.repository.AbstractRepository

import scala.util.{ Failure, Success, Try }

class LegalStatusService(repository: AbstractRepository) extends BusinessService with BusinessRequestValidation {

  override def validateRequest(validatedLocalMessage: ValidatedLocalMessage): ValidationResult[RequestMessage] =
    Validated.valid(ListAssetLegalStatusSummary())

  override def process(request: RequestMessage): Either[BusinessServiceError, BusinessServiceReply] =
    Try(request.asInstanceOf[ListAssetLegalStatusSummary]) match {
      case Success(_) => Right(LegalStatusReply(getLegalStatusSummaries.asJson.toString()))
      case Failure(e) => Left(LegalStatusError(e.getMessage))
    }

  private def getLegalStatusSummaries: List[LegalStatusSummary] =
    repository.getLegalStatusEntities match {
      case Success(legalStatusEntities) =>
        legalStatusEntities.flatMap { agentEntity =>
          agentEntity.as[Option[LegalStatusSummary]]
        }
      case _ => List.empty // TODO (RW) log the error
    }

}
