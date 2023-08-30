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

package uk.gov.nationalarchives.omega.api.business.records

import io.circe.syntax.EncoderOps
import uk.gov.nationalarchives.omega.api.business.agents.{ListAgentSummaryError, ListAgentSummaryReply}
import uk.gov.nationalarchives.omega.api.business.{BusinessService, BusinessServiceError, BusinessServiceReply}
import uk.gov.nationalarchives.omega.api.common.ServiceException
import uk.gov.nationalarchives.omega.api.messages.reply.{AgentSummary, RecordFull}
import uk.gov.nationalarchives.omega.api.messages.request.{GetRecordFull, ListAgentSummary, RequestMessage}
import uk.gov.nationalarchives.omega.api.repository.AbstractRepository
import uk.gov.nationalarchives.omega.api.repository.model.AgentConceptEntity

import scala.util.{Failure, Success, Try}

class GetRecordFullService(val repository: AbstractRepository) extends BusinessService {
  override def process(request: RequestMessage): Either[BusinessServiceError, BusinessServiceReply] = {
    val record = for {
      getFullRecordRequest <- Try(request.asInstanceOf[GetRecordFull])
      recordFull <- getFullRecord(getFullRecordRequest)
    } yield recordFull
    record match {
      case Success(rec) => Right(GetRecordFullReply(rec.asJson.toString()))
      case Failure(exception) => Left(GetRecordFullError(exception.getMessage))
    }
  }

  private def getFullRecord(recordRequest: GetRecordFull): Try[RecordFull] =
    for {
      recordFullEntity <- repository.getRecordFullEntity(recordRequest)
      recordFull <- convertRecordFullEntity(recordFullEntity)
    } yield recordFull

  private def convertRecordFullEntities(
                                           agentSummaryEntities: List[AgentConceptEntity],
                                           listAgentSummary: ListAgentSummary
                                         ): Try[RecordFull] =
    agentSummaryEntities.map { agentSummaryEntity =>
      agentSummaryEntity.as[Option[AgentSummary]] match {
        case Some(agentSummary) =>
          for {
            agentDescriptions <- getAgentDescriptions(agentSummaryEntity, listAgentSummary)
            agentSummaryWithDescriptions <- combineSummaryAndDescriptions(agentSummary, agentDescriptions)
          } yield agentSummaryWithDescriptions
        case _ => Failure(ServiceException("Failed to transform AgentSummaryEntity to AgentSummary"))
      }
    }.sequence
}
