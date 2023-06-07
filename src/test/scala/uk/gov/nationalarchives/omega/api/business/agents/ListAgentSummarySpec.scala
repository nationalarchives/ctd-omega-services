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

package uk.gov.nationalarchives.omega.api.business.agents

import cats.data.Chain
import cats.data.Validated.{ Invalid, Valid }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.business.InvalidAgentSummaryRequestError
import uk.gov.nationalarchives.omega.api.messages.AgentType.CorporateBody
import uk.gov.nationalarchives.omega.api.models.ListAgentSummary
import uk.gov.nationalarchives.omega.api.support.TestStubData

class ListAgentSummarySpec extends AnyFreeSpec with Matchers {

  val stubData = new TestStubData

  val listAgentSummaryService = new ListAgentSummaryService(stubData)

  "The ListAgentSummaryService" - {
    "returns a result on processRequest when given" - {
      "a valid listAgentSummaryRequest" in {

        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody","Person"],
                  |    "authorityFile" : false,
                  |    "depository" : false,
                  |    "version" : "all"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.process(listAgentSummaryRequest)

        result mustBe Right(ListAgentSummaryReply(getExpectedAgentSummaries))
      }

      "an empty payload request" in {

        val listAgentSummaryRequest = ListAgentSummaryRequest()
        val result = listAgentSummaryService.process(listAgentSummaryRequest)

        result mustBe Right(ListAgentSummaryReply(getExpectedAgentSummaries))
      }

    }

    "returns an error on validateRequest when given" - {
      "an invalid AgentType " in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody2","Person"],
                  |    "authorityFile" : true,
                  |    "depository" : false,
                  |    "version" : "latest"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)

        result mustBe Invalid(
          Chain(
            InvalidAgentSummaryRequestError(
              "Error decoding request message"
            )
          )
        )
      }

      "null value for authorityFile" in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody"],
                  |    "authorityFile" : "",
                  |    "depository" : false,
                  |    "version" : "latest"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)

        result mustBe Invalid(
          Chain(
            InvalidAgentSummaryRequestError(
              "Error decoding request message"
            )
          )
        )

      }

      "non Boolean value for authorityFile" in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody"],
                  |    "authorityFile" : maybe,
                  |    "depository" : false,
                  |    "version" : "latest"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)

        result mustBe Invalid(
          Chain(
            InvalidAgentSummaryRequestError(
              "Error decoding request message"
            )
          )
        )
      }
      "unrecognised version identifier" in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody"],
                  |    "authorityFile" : false,
                  |    "depository" : false,
                  |    "version" : "latest1"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)

        result mustBe Invalid(
          Chain(
            InvalidAgentSummaryRequestError(
              "Error parsing invalid input date"
            )
          )
        )
      }
      "invalid version timestamp" in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody"],
                  |    "authorityFile" : false,
                  |    "depository" : false,
                  |    "version" : "2020-05-19"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)

        result mustBe Invalid(
          Chain(
            InvalidAgentSummaryRequestError(
              "Error parsing invalid input date"
            )
          )
        )
      }
    }

    "returns valid result on validateRequest when given" - {
      "valid version timestamp" in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody"],
                  |    "authorityFile" : false,
                  |    "depository" : false,
                  |    "version" : "2022-06-22T02:00:00-0500"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)
        result mustBe Valid(
          ListAgentSummaryRequest(
            None,
            Some(ListAgentSummary(List(CorporateBody), Some(false), Some(false), "2022-06-22T02:00:00-0500"))
          )
        )
      }
      "valid version identifier" in {
        val listAgentSummaryRequest = ListAgentSummaryRequest(
          Some(s"""{
                  |    "agentType" : ["CorporateBody"],
                  |    "authorityFile" : false,
                  |    "depository" : false,
                  |    "version" : "latest"
                  |}""".stripMargin)
        )
        val result = listAgentSummaryService.validateRequest(listAgentSummaryRequest)
        result mustBe Valid(
          ListAgentSummaryRequest(
            None,
            Some(ListAgentSummary(List(CorporateBody), Some(false), Some(false), "latest"))
          )
        )
      }
    }
  }

  private def getExpectedAgentSummaries: String = s"""[
  {
    "agentType" : "Person",
    "identifier" : "48N",
    "label" : "Baden-Powell, Lady Olave St Clair",
    "dateFrom" : "1889",
    "dateTo" : "1977"
  },
  {
    "agentType" : "Person",
    "identifier" : "46F",
    "label" : "Fawkes, Guy",
    "dateFrom" : "1570",
    "dateTo" : "1606"
  },
  {
    "agentType" : "CorporateBody",
    "identifier" : "92W",
    "label" : "Joint Milk Quality Committee",
    "dateFrom" : "1948",
    "dateTo" : "1948"
  },
  {
    "agentType" : "CorporateBody",
    "identifier" : "8R6",
    "label" : "Queen Anne's Bounty",
    "dateFrom" : "",
    "dateTo" : ""
  }
]""".stripMargin

}
