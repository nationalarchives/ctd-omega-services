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

package uk.gov.nationalarchives.omega.api.business.legalstatus

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.support.TestStubData

class LegalStatusServiceSpec extends AnyFreeSpec with Matchers {

  val stubData = new TestStubData
  val legalStatusService = new LegalStatusService(stubData)

  "The LegalStatusService" - {
    "when receives" - {
      "a valid LegalStatusRequest" in {
        val legalStatusRequest = LegalStatusRequest()

        val result = legalStatusService.process(legalStatusRequest)

        result mustBe Right(LegalStatusReply(s"""[
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/public-record",
    "name" : "Public Record"
  },
  {
    "identifier" : "http://catalogue.nationalarchives.gov.uk/non-public-record",
    "name" : "Non-Public Record"
  }
]""".stripMargin))
      }
    }
  }

}
