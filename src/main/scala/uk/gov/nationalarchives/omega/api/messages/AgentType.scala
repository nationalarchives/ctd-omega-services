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

package uk.gov.nationalarchives.omega.api.messages

import enumeratum.EnumEntry.CapitalWords
import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.util.{Failure, Success, Try}

sealed trait AgentType extends EnumEntry with CapitalWords

object AgentType extends Enum[AgentType] with CirceEnum[AgentType] {

  val values = findValues

  case object CorporateBody extends AgentType

  case object Person extends AgentType

  case object CollectiveAgent extends AgentType

  case object Family extends AgentType

  case object HardwareAgent extends AgentType

  case object SoftwareAgent extends AgentType

  def fromUriString(uriString: String): Try[AgentType] = uriString match {
    case "http://cat.nationalarchives.gov.uk/corporate-body-concept" => Success(CorporateBody)
    case "http://cat.nationalarchives.gov.uk/person-concept" => Success(Person)
    case "http://cat.nationalarchives.gov.uk/collective-agent-concept" => Success(CollectiveAgent)
    case "http://cat.nationalarchives.gov.uk/family-concept" => Success(Family)
    case "http://cat.nationalarchives.gov.uk/hardware-agent-concept" => Success(HardwareAgent)
    case "http://cat.nationalarchives.gov.uk/software-agent-concept" => Success(SoftwareAgent)
    case unknown => Failure(UnknownAgentTypeException(s"Unknown agent type: $unknown"))
  }

}

case class UnknownAgentTypeException(message: String) extends Exception(message)
