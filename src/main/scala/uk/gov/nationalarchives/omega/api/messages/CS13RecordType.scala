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
import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.repository.BaseURL

import scala.util.{ Failure, Success, Try }

sealed trait CS13RecordType extends EnumEntry with CapitalWords

object CS13RecordType extends Enum[CS13RecordType] with CirceEnum[CS13RecordType] {

  val values = findValues

  case object Piece extends CS13RecordType

  case object Item extends CS13RecordType

  def fromUri(cs13RecordTypeUri: URI): Try[CS13RecordType] =
    cs13RecordTypeUri.toString match {
      case s"${BaseURL.cat}/piece" => Success(Piece)
      case s"${BaseURL.cat}/item"  => Success(Item)
      case unknown                 => Failure(new IllegalArgumentException(s"Unknown CS13 record type: $unknown"))
    }

}
