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
import enumeratum.{ CirceEnum, Enum, EnumEntry }
import uk.gov.nationalarchives.omega.api.repository.BaseURL

import scala.util.{ Failure, Success, Try }

sealed trait RecordType extends EnumEntry with CapitalWords

object RecordType extends Enum[RecordType] with CirceEnum[RecordType] {

  val values = findValues

  case object Physical extends RecordType

  case object Digitised extends RecordType

  case object BornDigital extends RecordType

  case object Hybrid extends RecordType

  def fromUri(uri: String): Try[RecordType] =
    uri match {
      case s"${BaseURL.tna}/ont.physical-record" => Success(Physical)
      case unknown => Failure(new IllegalArgumentException(s"Unknown record type: $unknown"))
    }

}
