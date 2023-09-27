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

package uk.gov.nationalarchives.omega.api.common

import enumeratum.{ CirceEnum, Enum, EnumEntry }

sealed trait ErrorCode extends EnumEntry
object ErrorCode extends Enum[ErrorCode] with CirceEnum[ErrorCode] {

  val values: IndexedSeq[ErrorCode] = findValues

  /** Echo processing error */
  case object PROC001 extends ErrorCode

  /** Legal Status processing error */
  case object PROC002 extends ErrorCode

  /** Agent processing error */
  case object PROC003 extends ErrorCode

  /** Record processing error */
  case object PROC004 extends ErrorCode

  /** Missing JMSMessageID error */
  case object MISS001 extends ErrorCode

  /** Missing OMGMessageTypeID error */
  case object MISS002 extends ErrorCode

  /** Missing OMGApplicationID error */
  case object MISS003 extends ErrorCode

  /** Missing JMSTimestamp error */
  case object MISS004 extends ErrorCode

  /** Missing OMGMessageFormat error */
  case object MISS005 extends ErrorCode

  /** Missing OMGToken error */
  case object MISS006 extends ErrorCode

  /** Missing OMGReplyAddress error */
  case object MISS007 extends ErrorCode

  /** Invalid JMSMessageID error */
  case object INVA001 extends ErrorCode

  /** Invalid OMGMessageTypeID error */
  case object INVA002 extends ErrorCode

  /** Invalid OMGApplicationID error */
  case object INVA003 extends ErrorCode

  /** Invalid OMGMessageFormat error */
  case object INVA005 extends ErrorCode

  /** Invalid OMGToken error */
  case object INVA006 extends ErrorCode

  /** Invalid OMGReplyAddress error */
  case object INVA007 extends ErrorCode

  /** General invalid error */
  case object INVA008 extends ErrorCode

}
