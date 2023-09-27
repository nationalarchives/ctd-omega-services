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

sealed trait IncomingMessageType extends EnumEntry
object IncomingMessageType extends Enum[IncomingMessageType] {

  val values: IndexedSeq[IncomingMessageType] = findValues

  /** Echo message type */
  case object ECHO001 extends IncomingMessageType {
    // This happens to follow the regex; otherwise, it's arbitrary.
    override val entryName = "OSGESZZZ100"
  }

  /** List Asset Legal Status Summaries message type */
  case object OSLISALS001 extends IncomingMessageType {
    override val entryName = "OSLISALS001"
  }

  /** List Agent Summaries message type */
  case object OSLISAGT001 extends IncomingMessageType {
    override val entryName = "OSLISAGT001"
  }

  /** Get Record message type */
  case object OSGEFREC001 extends IncomingMessageType {
    override val entryName = "OSGEFREC001"
  }
  // add more service identifiers here

}
