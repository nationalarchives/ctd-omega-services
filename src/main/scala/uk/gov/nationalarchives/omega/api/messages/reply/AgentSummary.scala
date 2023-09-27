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

package uk.gov.nationalarchives.omega.api.messages.reply

import io.circe.syntax._
import io.circe.{ Encoder, Json }
import uk.gov.nationalarchives.omega.api.messages.AgentType

import java.time.ZonedDateTime

/** Represents an AgentSummary as defined by the API services */
case class AgentSummary(
  agentType: AgentType,
  identifier: String,
  currentDescription: String,
  descriptions: List[AgentDescription]
) extends ReplyMessage
object AgentSummary {
  implicit val encodeAgentSummary: Encoder[AgentSummary] = (agentSummary: AgentSummary) =>
    Json
      .obj(
        ("type", Json.fromString(agentSummary.agentType.entryName)),
        ("identifier", Json.fromString(agentSummary.identifier)),
        ("current-description", Json.fromString(agentSummary.currentDescription)),
        ("description", agentSummary.descriptions.asJson)
      )
      .deepDropNullValues

}

case class AgentDescription(
  identifier: String,
  label: String,
  versionTimestamp: ZonedDateTime,
  authorityFile: Option[Boolean] = None,
  depository: Option[Boolean] = None,
  dateFrom: Option[String] = None,
  dateTo: Option[String] = None,
  previousDescription: Option[String] = None
)
object AgentDescription {
  implicit val encodeAgentDescription: Encoder[AgentDescription] = (agentDescription: AgentDescription) =>
    Json.obj(
      ("identifier", agentDescription.identifier.asJson),
      ("label", agentDescription.label.asJson),
      ("authority-file", agentDescription.authorityFile.asJson),
      ("depository", agentDescription.depository.asJson),
      ("version-timestamp", agentDescription.versionTimestamp.asJson),
      ("date-from", agentDescription.dateFrom.asJson),
      ("date-to", agentDescription.dateTo.asJson),
      ("previous-description", agentDescription.previousDescription.asJson)
    )
}
