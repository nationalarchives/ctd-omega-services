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
import uk.gov.nationalarchives.omega.api.messages.RecordType
import uk.gov.nationalarchives.omega.api.messages.reply.GenericIdentifierDerivation._

/** Represents a RecordFull as defined by the API schema */
case class RecordFull(
  identifier: Identifier,
  recordType: RecordType,
  creators: List[String],
  currentDescription: Identifier,
  descriptions: List[RecordDescriptionFull]
) extends ReplyMessage
object RecordFull {

  implicit val encodeRecordFull: Encoder[RecordFull] = (recordFull: RecordFull) =>
    Json
      .obj(
        ("identifier", recordFull.identifier.asJson),
        ("type", Json.fromString(recordFull.recordType.entryName)),
        ("creator", recordFull.creators.asJson),
        ("current-description", recordFull.currentDescription.asJson),
        ("description", recordFull.descriptions.asJson)
      )
      .deepDropNullValues

}
