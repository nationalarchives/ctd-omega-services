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
import cats.effect.IO
import cats.implicits._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.common.ServiceException
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{ InvalidMessagePayload, MessageValidationError, ValidationResult }
import uk.gov.nationalarchives.omega.api.messages.ValidatedLocalMessage
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentDescription, AgentSummary }
import uk.gov.nationalarchives.omega.api.messages.request.{ ListAgentSummary, RequestMessage }
import uk.gov.nationalarchives.omega.api.repository.AbstractRepository
import uk.gov.nationalarchives.omega.api.repository.model.AgentConceptEntity

import java.time.ZonedDateTime
import scala.util.Try

class ListAgentSummaryService(val repository: AbstractRepository)
    extends BusinessService with BusinessRequestValidation {

  override def process(
    requestMessage: RequestMessage
  ): IO[Either[BusinessServiceError, BusinessServiceReply]] = {
    for {
      listAgentSummary <- IO(requestMessage.asInstanceOf[ListAgentSummary])
      agentSummaries   <- getAgentSummaries(listAgentSummary)
    } yield agentSummaries
  }.redeem(
    error => Left(ListAgentSummaryError(error.getMessage)),
    sums => Right(ListAgentSummaryReply(sums.asJson.toString()))
  )

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
      Validated.valid(ListAgentSummary())
    }

  private def validateDate(dateStr: String): Option[ZonedDateTime] = {
    val trimmedDate = dateStr.trim
    if (trimmedDate.isEmpty) None
    else
      Try(ZonedDateTime.parse(trimmedDate)).toOption
  }

  private def getAgentSummaries(listAgentSummary: ListAgentSummary): IO[List[AgentSummary]] =
    for {
      agentEntities  <- repository.getAgentSummaryEntities(listAgentSummary)
      agentSummaries <- convertAgentSummaryEntities(agentEntities, listAgentSummary)
    } yield agentSummaries

  private def combineSummaryAndDescriptions(
    agentSummary: AgentSummary,
    agentDescriptions: List[AgentDescription]
  ): IO[AgentSummary] =
    IO(agentSummary.copy(descriptions = agentDescriptions))

  private def convertAgentSummaryEntities(
    agentSummaryEntities: List[AgentConceptEntity],
    listAgentSummary: ListAgentSummary
  ): IO[List[AgentSummary]] =
    agentSummaryEntities.map { agentSummaryEntity =>
      getAgentSummary(agentSummaryEntity, listAgentSummary)
    }.sequence

  private def getAgentSummary(
    agentSummaryEntity: AgentConceptEntity,
    listAgentSummary: ListAgentSummary
  ): IO[AgentSummary] =
    agentSummaryEntity.as[IO[Option[AgentSummary]]].flatMap { agentSummaryOpt =>
      agentSummaryOpt match {
        case Some(agentSummary) =>
          for {
            agentDescriptions            <- getAgentDescriptions(agentSummaryEntity, listAgentSummary)
            agentSummaryWithDescriptions <- combineSummaryAndDescriptions(agentSummary, agentDescriptions)
          } yield agentSummaryWithDescriptions
        case _ => IO.raiseError(ServiceException("Failed to transform AgentSummaryEntity to AgentSummary"))
      }
    }

  private def getAgentDescriptions(
    agentSummaryEntity: AgentConceptEntity,
    listAgentSummary: ListAgentSummary
  ): IO[List[AgentDescription]] =
    for {
      agentDescriptionEntities <- repository.getAgentDescriptionEntities(listAgentSummary, agentSummaryEntity.conceptId)
      agentDescriptions <-
        IO(agentDescriptionEntities.map(agentDescriptionEntity => agentDescriptionEntity.as[AgentDescription]))
    } yield agentDescriptions

}
