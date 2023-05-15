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

import enumeratum._

sealed trait OutgoingMessageType extends EnumEntry
object OutgoingMessageType extends Enum[OutgoingMessageType] {

  val values: IndexedSeq[OutgoingMessageType] = findValues

  /** The lowest common denominator of error where it is unclear what went wrong
    */
  case object GeneralError extends OutgoingMessageType {
    override val entryName = "OELIFERR001"
  }

  /** An error occured during early stage parsing/recognition of the message. This includes extracting and validating
    * the presence of required message headers and that they meet their excpected data types, extracting the body as
    * text and validating that its format (NOT content) is correct (i.e. that it is valid JSON/XML/RDF/Binary etc.)
    */
  case object InvalidMessageFormatError extends OutgoingMessageType {
    override val entryName = "OELIFERR002"
  }

  /** The client application as identified by the `OMGApplicationID` header is either invalid, not-recognised, or
    * prohibited.
    */
  case object InvalidApplicationError extends OutgoingMessageType {
    override val entryName = "OELIFERR003"
  }

  /** Authentication failed, likely due to an invalid token being present in the `OMGToken` header.
    */
  case object AuthenticationError extends OutgoingMessageType {
    override val entryName = "OELIFERR004"
  }

  /** The value of `OMGMessageTypeID` header was not recognised and so the message cannot be further dispatched for
    * processing.
    */
  case object UnrecognisedMessageTypeError extends OutgoingMessageType {
    override val entryName = "OELIFERR005"
  }

  /** The content of the body of the message has failed validation, e.g. a parsed JSON message may be missing some
    * required properties, or those properties may have invalid values.
    */
  case object InvalidMessageError extends OutgoingMessageType {
    override val entryName = "OELIFERR006"
  }

  /** The received message was valid, but the system was unable complete the processing of a message.
    */
  case object ProcessingError extends OutgoingMessageType {
    override val entryName = "OELIFERR007"
  }

  /** Message Id for the legal status summary reply AssetLegalStatusSummaryList response as described in the AsyncAPI
    * schema
    */
  case object AssetLegalStatusSummaryList extends OutgoingMessageType {
    override val entryName = "ODLISALS001"
  }
}
