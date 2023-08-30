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
import uk.gov.nationalarchives.omega.api.messages.{ CS13RecordType, DescribedTemporal }
import uk.gov.nationalarchives.omega.api.messages.reply.GenericIdentifierDerivation._

case class RecordDescriptionFull(
  recordDescriptionSummary: RecordDescriptionSummary,
  assetLegalStatus: Option[Identifier],
  legacyTnaCs13RecordType: Option[CS13RecordType],
  designationOfEdition: Option[String],
  created: Option[DescribedTemporal],
  covering: Option[DescribedTemporal],
  archivistsNote: Option[String],
  sourceOfAcquisition: Option[String],
  custodialHistory: Option[String],
  administrativeBiographicalBackground: Option[String],
  accumulation: Option[DescribedTemporal],
  appraisal: Option[String],
  accrualPolicy: Option[String],
  layout: Option[String],
  publicationNote: Option[String],
  referencedBy: Option[Identifier],
  relatedTo: Option[Identifier],
  separatedFrom: Option[Identifier],
  subject: Option[Identifier]
)
object RecordDescriptionFull {
  implicit val encodeRecordDescriptionFull: Encoder[RecordDescriptionFull] =
    (recordDescriptionFull: RecordDescriptionFull) =>
      Json
        .obj(
          ("identifier", recordDescriptionFull.recordDescriptionSummary.identifier.asJson),
          ("secondary-identifier", recordDescriptionFull.recordDescriptionSummary.secondaryIdentifier.asJson),
          ("label", Json.fromString(recordDescriptionFull.recordDescriptionSummary.label)),
          ("abstract", Json.fromString(recordDescriptionFull.recordDescriptionSummary.description)),
          ("access-rights", recordDescriptionFull.recordDescriptionSummary.accessRights.asJson),
          ("is-part-of", recordDescriptionFull.recordDescriptionSummary.isPartOf.asJson),
          ("previous-sibling", recordDescriptionFull.recordDescriptionSummary.previousSibling.asJson),
          ("version-timestamp", Json.fromString(recordDescriptionFull.recordDescriptionSummary.versionTimestamp)),
          ("previous-description", recordDescriptionFull.recordDescriptionSummary.previousDescription.asJson),
          ("asset-legal-status", recordDescriptionFull.assetLegalStatus.asJson),
          ("legacy-tna-cs13-record-type", recordDescriptionFull.legacyTnaCs13RecordType.asJson),
          ("designation-of-edition", recordDescriptionFull.designationOfEdition.asJson),
          ("created", recordDescriptionFull.created.asJson),
          ("covering", recordDescriptionFull.covering.asJson),
          ("archivists-note", recordDescriptionFull.archivistsNote.asJson),
          ("source-of-acquisition", recordDescriptionFull.sourceOfAcquisition.asJson),
          ("custodial-history", recordDescriptionFull.custodialHistory.asJson),
          ("administrative-biographical-background", recordDescriptionFull.administrativeBiographicalBackground.asJson),
          ("accumulation", recordDescriptionFull.accumulation.asJson),
          ("appraisal", recordDescriptionFull.appraisal.asJson),
          ("accrual-policy", recordDescriptionFull.accrualPolicy.asJson),
          ("layout", recordDescriptionFull.layout.asJson),
          ("publication-note", recordDescriptionFull.publicationNote.asJson),
          ("referenced-by", recordDescriptionFull.referencedBy.asJson),
          ("related-to", recordDescriptionFull.relatedTo.asJson),
          ("separated-from", recordDescriptionFull.separatedFrom.asJson),
          ("subject", recordDescriptionFull.subject.asJson)
        )
        .deepDropNullValues

}
