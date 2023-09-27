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

package uk.gov.nationalarchives.omega.api.support

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import jms4s.config.QueueName
import org.mockito.MockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.messages.ValidatedLocalMessage

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait AsyncUnitTest extends AsyncFreeSpec with AsyncIOSpec with Matchers with MockitoSugar {

  /** Helper method to get a ValidatedLocalMessage instance
    * @param messageText
    *   the body of the message
    * @return
    */
  def getValidatedLocalMessage(messageText: String): ValidatedLocalMessage =
    ValidatedLocalMessage(Version1UUID.generate(), "", messageText, "", "", LocalDateTime.now(), "", "", QueueName(""))

  def await[T](io: IO[T]): T =
    Await.result(io.unsafeToFuture(), 60.second)
}
