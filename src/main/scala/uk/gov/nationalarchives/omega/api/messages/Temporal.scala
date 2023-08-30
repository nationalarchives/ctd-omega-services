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

import io.circe.{ Encoder, Json }
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

sealed trait Temporal

case class TemporalInstant(instant: String) extends Temporal
case class TemporalInterval(dateFrom: String, dateTo: String) extends Temporal

object GenericTemporalDerivation {
  implicit val encodeTemporal: Encoder[Temporal] = Encoder.instance {
    case instant @ TemporalInstant(_) => instant.asJson
    case TemporalInterval(dateFrom, dateTo) =>
      Json.obj(
        ("date-from", Json.fromString(dateFrom)),
        ("date-to", Json.fromString(dateTo))
      )
  }
}
