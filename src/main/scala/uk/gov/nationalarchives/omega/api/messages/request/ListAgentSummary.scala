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

package uk.gov.nationalarchives.omega.api.messages.request

import io.circe.syntax._
import io.circe.{ Decoder, Encoder, Json }
import uk.gov.nationalarchives.omega.api.messages.AgentType

case class ListAgentSummary(
  agentTypes: List[AgentType],
  versionTimestamp: Option[String] = None,
  depository: Option[Boolean] = None,
  authorityFile: Option[Boolean] = None
) extends RequestMessage

object ListAgentSummary {
  implicit val encodeListAgentSummary: Encoder[ListAgentSummary] = (listAgentSummary: ListAgentSummary) =>
    Json
      .obj(
        ("type", listAgentSummary.agentTypes.asJson),
        ("depository", listAgentSummary.depository.asJson),
        ("authority-file", listAgentSummary.authorityFile.asJson),
        ("version-timestamp", listAgentSummary.versionTimestamp.asJson)
      )
      .dropNullValues

  implicit val decodeListAgentSummary: Decoder[ListAgentSummary] = json =>
    for {
      agentType     <- json.get[List[AgentType]]("type")
      depository    <- json.get[Option[Boolean]]("depository")
      authorityFile <- json.get[Option[Boolean]]("authority-file")
      version       <- json.get[Option[String]]("version-timestamp")
    } yield ListAgentSummary(agentType, version, depository, authorityFile)

}
