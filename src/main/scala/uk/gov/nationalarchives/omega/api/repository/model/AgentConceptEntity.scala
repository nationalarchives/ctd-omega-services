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

package uk.gov.nationalarchives.omega.api.repository.model

import cats.effect.IO
import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.common.AppLogger
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentDescription, AgentSummary }

import scala.util.Success

case class AgentConceptEntity(conceptId: URI, agentType: URI, currentVersionId: URI) {
  def as[T](implicit f: AgentConceptEntity => T): T = f(this)
}
object AgentConceptEntity extends AgentTypeMapper with AppLogger {

  implicit def agentSummaryMapper: AgentConceptEntity => IO[Option[AgentSummary]] =
    (agentSummaryEntity: AgentConceptEntity) =>
      getAgentTypeFromUri(agentSummaryEntity.agentType) match {
        case Success(agentType) =>
          IO(
            Some(
              AgentSummary(
                agentType,
                agentSummaryEntity.conceptId.toString,
                agentSummaryEntity.currentVersionId.toString,
                List.empty[AgentDescription]
              )
            )
          )
        case _ => logger.error(s"Unknown agent type: ${agentSummaryEntity.agentType.toString}") *> IO.pure(None)
      }

}
