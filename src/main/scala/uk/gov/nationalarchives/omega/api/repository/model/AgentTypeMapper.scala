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
import uk.gov.nationalarchives.omega.api.repository.BaseURL

import scala.util.{ Failure, Success, Try }

trait AgentTypeMapper {

  def getAgentTypeFromUri(agentTypeUri: URI): Try[AgentType] =
    agentTypeUri.toString match {
      case s"${BaseURL.cat}/person-concept"           => Success(Person)
      case s"${BaseURL.cat}/family-concept"           => Success(Family)
      case s"${BaseURL.cat}/corporate-body-concept"   => Success(CorporateBody)
      case s"${BaseURL.cat}/collective-agent-concept" => Success(CollectiveAgent)
      case s"${BaseURL.cat}/hardware-agent-concept"   => Success(HardwareAgent)
      case s"${BaseURL.cat}/software-agent-concept"   => Success(SoftwareAgent)
      case unknown => Failure(new IllegalArgumentException(s"Unknown agent type: $unknown"))
    }

  def getUriFromAgentType(agentType: AgentType): String =
    agentType match {
      case Person          => s"${BaseURL.cat}/person-concept"
      case Family          => s"${BaseURL.cat}/family-concept"
      case CorporateBody   => s"${BaseURL.cat}/corporate-body-concept"
      case CollectiveAgent => s"${BaseURL.cat}/collective-agent-concept"
      case HardwareAgent   => s"${BaseURL.cat}/hardware-agent-concept"
      case SoftwareAgent   => s"${BaseURL.cat}/software-agent-concept"
    }

  def getAllAgentTypes: List[AgentType] =
    List(Person, Family, CorporateBody, CollectiveAgent, HardwareAgent, SoftwareAgent)

}
