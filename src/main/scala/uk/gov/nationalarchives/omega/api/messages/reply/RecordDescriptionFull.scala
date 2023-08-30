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
import uk.gov.nationalarchives.omega.api.messages.reply.GenericIdentifierDerivation._

case class RecordDescriptionFull(
  summary: RecordDescriptionSummary,
  properties: RecordDescriptionProperties
)
object RecordDescriptionFull {
  implicit val encodeRecordDescriptionFull: Encoder[RecordDescriptionFull] =
    (record: RecordDescriptionFull) =>
      Json
        .obj(
          ("identifier", record.summary.identifier.asJson),
          ("secondary-identifier", record.summary.secondaryIdentifier.asJson),
          ("label", Json.fromString(record.summary.label)),
          ("abstract", Json.fromString(record.summary.description)),
          ("access-rights", record.summary.accessRights.asJson),
          ("is-part-of", record.summary.isPartOf.asJson),
          ("previous-sibling", record.summary.previousSibling.asJson),
          ("version-timestamp", Json.fromString(record.summary.versionTimestamp)),
          ("previous-description", record.summary.previousDescription.asJson),
          ("asset-legal-status", record.properties.assetLegalStatus.asJson),
          ("legacy-tna-cs13-record-type", record.properties.legacyTnaCs13RecordType.asJson),
          ("designation-of-edition", record.properties.designationOfEdition.asJson),
          ("created", record.properties.created.asJson),
          ("covering", record.properties.covering.asJson),
          ("archivists-note", record.properties.archivistsNote.asJson),
          ("source-of-acquisition", record.properties.sourceOfAcquisition.asJson),
          ("custodial-history", record.properties.custodialHistory.asJson),
          ("administrative-biographical-background", record.properties.administrativeBiographicalBackground.asJson),
          ("accumulation", record.properties.accumulation.asJson),
          ("appraisal", record.properties.appraisal.asJson),
          ("accrual-policy", record.properties.accrualPolicy.asJson),
          ("layout", record.properties.layout.asJson),
          ("publication-note", record.properties.publicationNote.asJson),
          ("referenced-by", record.properties.referencedBy.asJson),
          ("related-to", record.properties.relatedTo.asJson),
          ("separated-from", record.properties.separatedFrom.asJson),
          ("subject", record.properties.subject.asJson)
        )
        .deepDropNullValues

}
