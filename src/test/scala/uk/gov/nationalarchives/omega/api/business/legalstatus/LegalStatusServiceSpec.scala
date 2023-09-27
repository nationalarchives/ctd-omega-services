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

import org.mockito.MockitoSugar
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.messages.request.ListAssetLegalStatusSummary
import uk.gov.nationalarchives.omega.api.repository.TestRepository
import uk.gov.nationalarchives.omega.api.repository.vocabulary.Cat

class LegalStatusServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val testRepository = new TestRepository
  private val legalStatusService = new LegalStatusService(testRepository)

  "The LegalStatusService" - {
    "when receives" - {
      "a valid LegalStatusRequest" in {
        val legalStatusRequest = ListAssetLegalStatusSummary()
        val result = legalStatusService.process(legalStatusRequest)

        result mustBe Right(LegalStatusReply(s"""[
  {
    "identifier" : "${Cat.publicRecord}",
    "label" : "Public Record"
  },
  {
    "identifier" : "${Cat.nonPublicRecord}",
    "label" : "Non-Public Record"
  },
  {
    "identifier" : "${Cat.publicRecordUnlessOtherwiseStated}",
    "label" : "Public Record (unless otherwise stated)"
  },
  {
    "identifier" : "${Cat.welshPublicRecord}",
    "label" : "Welsh Public Record"
  },
  {
    "identifier" : "${Cat.nonRecordMaterial}",
    "label" : "Non-Record Material"
  }
]""".stripMargin))
      }
    }
    // TODO This test is ignored until the logging and error handling is completed
    "when a processing error occurs" ignore {
      val legalStatusRequest = ListAssetLegalStatusSummary()
      val errorMessage = "There was an error"
      val result = legalStatusService.process(legalStatusRequest)
      result mustBe Left(LegalStatusError(errorMessage))
    }
  }

}
