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

import io.circe.syntax.EncoderOps
import io.circe.{ Encoder, Json }
import uk.gov.nationalarchives.omega.api.messages.reply.GenericIdentifierDerivation._

/** Represents a RecordDescriptionSummary as defined by the API schema */
case class RecordDescriptionSummary(
  identifier: Identifier,
  label: String,
  description: String,
  accessRights: List[Identifier],
  isPartOf: List[Identifier],
  versionTimestamp: String,
  secondaryIdentifier: Option[List[GeneralTypedIdentifier]],
  previousSibling: Option[Identifier],
  previousDescription: Option[Identifier]
)
object RecordDescriptionSummary {
  implicit val encodeRecordDescriptionSummary: Encoder[RecordDescriptionSummary] =
    (recordDescriptionSummary: RecordDescriptionSummary) =>
      Json
        .obj(
          ("identifier", recordDescriptionSummary.identifier.asJson),
          ("secondary-identifier", recordDescriptionSummary.secondaryIdentifier.asJson),
          ("label", recordDescriptionSummary.label.asJson),
          ("abstract", recordDescriptionSummary.description.asJson),
          ("access-rights", recordDescriptionSummary.accessRights.asJson),
          ("is-part-of", recordDescriptionSummary.isPartOf.asJson),
          ("previous-sibling", recordDescriptionSummary.previousSibling.asJson),
          ("version-timestamp", recordDescriptionSummary.versionTimestamp.asJson),
          ("previous-description", recordDescriptionSummary.previousDescription.asJson)
        )
}
