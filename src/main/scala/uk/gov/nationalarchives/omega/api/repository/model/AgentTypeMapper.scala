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
import uk.gov.nationalarchives.omega.api.messages.AgentType
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CollectiveAgent, CorporateBody, Family, HardwareAgent, Person, SoftwareAgent }

import scala.util.{ Failure, Success, Try }

trait AgentTypeMapper {

  private val cataloguePrefix = "http://cat.nationalarchives.gov.uk"

  def getAgentTypeFromUri(agentTypeUri: URI): Try[AgentType] =
    agentTypeUri.toString match {
      case s"$cataloguePrefix/person-concept"           => Success(Person)
      case s"$cataloguePrefix/family-concept"           => Success(Family)
      case s"$cataloguePrefix/corporate-body-concept"   => Success(CorporateBody)
      case s"$cataloguePrefix/collective-agent-concept" => Success(CollectiveAgent)
      case s"$cataloguePrefix/hardware-agent-concept"   => Success(HardwareAgent)
      case s"$cataloguePrefix/software-agent-concept"   => Success(SoftwareAgent)
      case unknown => Failure(new IllegalArgumentException(s"Unknown agent type: $unknown"))
    }

  def getUriFromAgentType(agentType: AgentType): String =
    agentType match {
      case Person          => s"$cataloguePrefix/person-concept"
      case Family          => s"$cataloguePrefix/family-concept"
      case CorporateBody   => s"$cataloguePrefix/corporate-body-concept"
      case CollectiveAgent => s"$cataloguePrefix/collective-agent-concept"
      case HardwareAgent   => s"$cataloguePrefix/hardware-agent-concept"
      case SoftwareAgent   => s"$cataloguePrefix/software-agent-concept"
    }

  def getAllAgentTypes: List[AgentType] =
    List(Person, Family, CorporateBody, CollectiveAgent, HardwareAgent, SoftwareAgent)

}
