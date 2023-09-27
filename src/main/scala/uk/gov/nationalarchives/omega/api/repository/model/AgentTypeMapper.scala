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
import uk.gov.nationalarchives.omega.api.messages.AgentType._
import uk.gov.nationalarchives.omega.api.repository.vocabulary.Cat

import scala.util.{ Failure, Success, Try }

trait AgentTypeMapper {

  def getAgentTypeFromUri(agentTypeUri: URI): Try[AgentType] =
    agentTypeUri.toString match {
      case s"${Cat.NS}person-concept"           => Success(Person)
      case s"${Cat.NS}family-concept"           => Success(Family)
      case s"${Cat.NS}corporate-body-concept"   => Success(CorporateBody)
      case s"${Cat.NS}collective-agent-concept" => Success(CollectiveAgent)
      case s"${Cat.NS}hardware-agent-concept"   => Success(HardwareAgent)
      case s"${Cat.NS}software-agent-concept"   => Success(SoftwareAgent)
      case unknown => Failure(new IllegalArgumentException(s"Unknown agent type: $unknown"))
    }

  def getUriFromAgentType(agentType: AgentType): String =
    agentType match {
      case Person          => Cat.personConcept
      case Family          => Cat.familyConcept
      case CorporateBody   => Cat.corporateBodyConcept
      case CollectiveAgent => Cat.collectiveAgentConcept
      case HardwareAgent   => Cat.hardwareAgentConcept
      case SoftwareAgent   => Cat.softwareAgentConcept
    }

  def getAllAgentTypes: List[AgentType] =
    List(Person, Family, CorporateBody, CollectiveAgent, HardwareAgent, SoftwareAgent)

}
