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

import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CollectiveAgent, CorporateBody, Family, HardwareAgent, Person, SoftwareAgent }
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentDescription, AgentSummary }

import scala.util.{ Failure, Success }

case class AgentEntity(
  agentType: URI,
  identifier: URI,
  currentDescription: URI,
  label: String,
  versionTimestamp: String,
  dateFrom: Option[String] = None,
  dateTo: Option[String] = None,
  depository: Option[Boolean] = None,
  previousDescription: Option[String] = None
) {
  def as[T](implicit f: AgentEntity => T): T = f(this)

}

object AgentEntity {

  val cataloguePrefix = "http://cat.nationalarchives.gov.uk"
  implicit def agentMapper: AgentEntity => Option[AgentSummary] = (agentEntity: AgentEntity) =>
    getAgentTypeFromUri(agentEntity.agentType) match {
      case Success(agentType) =>
        Some(
          AgentSummary(
            agentType,
            agentEntity.identifier.toString,
            agentEntity.currentDescription.toString,
            AgentDescription(
              agentEntity.currentDescription.toString,
              agentEntity.label,
              agentEntity.versionTimestamp,
              None,
              agentEntity.depository,
              agentEntity.dateFrom,
              agentEntity.dateTo,
              agentEntity.previousDescription
            )
          )
        )
      case _ => None // TODO (RW) log error here
    }

  private def getAgentTypeFromUri(agentTypeUri: URI) =
    agentTypeUri.toString match {
      case s"$cataloguePrefix/corporate-body-concept"   => Success(CorporateBody)
      case s"$cataloguePrefix/person-concept"           => Success(Person)
      case s"$cataloguePrefix/collective-agent-concept" => Success(CollectiveAgent)
      case s"$cataloguePrefix/family-concept"           => Success(Family)
      case s"$cataloguePrefix/hardware-agent-concept"   => Success(HardwareAgent)
      case s"$cataloguePrefix/software-agent-concept"   => Success(SoftwareAgent)
      case unknown => Failure(new IllegalArgumentException(s"Unknown agent type: $unknown"))
    }

}
