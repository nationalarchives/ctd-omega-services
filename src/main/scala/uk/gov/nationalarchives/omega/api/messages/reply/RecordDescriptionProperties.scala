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

import uk.gov.nationalarchives.omega.api.messages.{ CS13RecordType, DescribedTemporal }

/** Represents a RecordDescriptionProperties as defined by the API schema */
case class RecordDescriptionProperties(
  assetLegalStatus: Option[Identifier] = None,
  legacyTnaCs13RecordType: Option[CS13RecordType] = None,
  designationOfEdition: Option[String] = None,
  created: Option[DescribedTemporal] = None,
  covering: Option[DescribedTemporal] = None,
  archivistsNote: Option[String] = None,
  sourceOfAcquisition: Option[Identifier] = None,
  custodialHistory: Option[String] = None,
  administrativeBiographicalBackground: Option[String] = None,
  accumulation: Option[DescribedTemporal] = None,
  appraisal: Option[String] = None,
  accrualPolicy: Option[Identifier] = None,
  layout: Option[String] = None,
  publicationNote: Option[String] = None,
  isReferencedBy: Option[List[Identifier]] = None,
  relatedTo: Option[List[Identifier]] = None,
  separatedFrom: Option[List[Identifier]] = None,
  subject: Option[List[Identifier]] = None
)
