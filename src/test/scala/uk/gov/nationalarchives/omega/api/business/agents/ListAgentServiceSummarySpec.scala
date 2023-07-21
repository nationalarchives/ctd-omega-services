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
import uk.gov.nationalarchives.omega.api.messages.AgentType
import uk.gov.nationalarchives.omega.api.messages.AgentType.CorporateBody
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.InvalidMessagePayload
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.TestRepository
import uk.gov.nationalarchives.omega.api.support.{ TestStubData, UnitTest }

class ListAgentServiceSummarySpec extends UnitTest {

  private val stubData = new TestStubData
  private val testRepository = new TestRepository
  private val listAgentSummaryService = new ListAgentSummaryService(stubData, testRepository)

  "The ListAgentSummaryService" - {
    "returns a result on processRequest when given" - {
      "a valid listAgentSummaryRequest" in {

        val listAgentSummaryRequest = ListAgentSummary(
          List(AgentType.CorporateBody, AgentType.Person),
          versionTimestamp = Some("all"),
          depository = Some(false),
          authorityFile = Some(false)
        )
        val result = listAgentSummaryService.process(listAgentSummaryRequest)
        result mustBe
          Right(ListAgentSummaryReply(getExpectedAgentSummaries))
      }

      "a valid listAgentSummaryRequest for place of deposit" in {

        val listAgentSummaryRequest = ListAgentSummary(
          List(AgentType.CorporateBody),
          versionTimestamp = Some("all"),
          depository = Some(true),
          authorityFile = Some(false)
        )
        val result = listAgentSummaryService.process(listAgentSummaryRequest)
        result mustBe
          Right(ListAgentSummaryReply(getExpectedPlaceOfDepositSummaries))
      }

      // NOTE (RW): This test is ignored as empty requests are not allowed by SQS although they are allowed by the schema
      // PACT-1025 refers
      "an empty payload request" ignore {

        val listAgentSummaryRequest = ListAgentSummary(List())
        val result = listAgentSummaryService.process(listAgentSummaryRequest)

        result mustBe Right(ListAgentSummaryReply(getExpectedAgentSummaries))
      }

    }

    "returns an error on validateRequest when given" - {
      "an invalid AgentType " in {
        val message = getValidatedLocalMessage(s"""{
                                                  |    "type" : ["CorporateBody2","Person"],
                                                  |    "authority-file" : true,
                                                  |    "depository" : false,
                                                  |    "version-timestamp" : "latest"
                                                  |}""".stripMargin)
        val result = listAgentSummaryService.validateRequest(message)
        result mustBe a[Invalid[_]]
      }

    }

    "null value for authorityFile" in {
      val message = getValidatedLocalMessage(
        s"""{
           |    "type" : ["Corporate Body"],
           |    "authority-file" : "",
           |    "depository" : false,
           |    "version-timestamp" : "latest"
           |}""".stripMargin
      )
      val result = listAgentSummaryService.validateRequest(message)
      result mustBe a[Invalid[_]]
    }

    "non Boolean value for authorityFile" in {
      val message = getValidatedLocalMessage(s"""{
                                                |    "type" : ["Corporate Body"],
                                                |    "authority-file" : maybe,
                                                |    "depository" : false,
                                                |    "version-timestamp" : "latest"
                                                |}""".stripMargin)
      val result = listAgentSummaryService.validateRequest(message)

      result mustBe a[Invalid[_]]
    }
    "unrecognised version identifier" in {
      val message = getValidatedLocalMessage(s"""{
                                                |    "type" : ["Corporate Body"],
                                                |    "authority-file" : false,
                                                |    "depository" : false,
                                                |    "version-timestamp" : "latest1"
                                                |}""".stripMargin)
      val result = listAgentSummaryService.validateRequest(message)

      result mustBe a[Invalid[_]]
    }
    "invalid version timestamp" in {
      val message = getValidatedLocalMessage(s"""{
                                                |    "type" : ["Corporate Body"],
                                                |    "authority-file" : false,
                                                |    "depository" : false,
                                                |    "version-timestamp" : "2020-05-19"
                                                |}""".stripMargin)
      val result = listAgentSummaryService.validateRequest(message)

      result mustBe Invalid(
        Chain(
          InvalidMessagePayload(Some("Invalid date: 2020-05-19"))
        )
      )
    }
  }

  "returns valid result on validateRequest when given" - {
    "valid version timestamp" in {
      val message = getValidatedLocalMessage(s"""{
                                                |    "type" : ["Corporate Body"],
                                                |    "authority-file" : false,
                                                |    "depository" : false,
                                                |    "version-timestamp" : "2022-06-22T02:00:00-0500"
                                                |}""".stripMargin)
      val result = listAgentSummaryService.validateRequest(message)

      result mustBe
        Valid(
          ListAgentSummary(List(CorporateBody), Some("2022-06-22T02:00:00-0500"), Some(false), Some(false))
        )
    }
    "no version timestamp and authority file" in {
      val message = getValidatedLocalMessage(s"""{
                                                |    "type" : ["Corporate Body"],
                                                |    "depository" : true
                                                |}""".stripMargin)
      val result = listAgentSummaryService.validateRequest(message)
      result mustBe Valid(ListAgentSummary(List(CorporateBody), None, Some(true), None))
    }
    "valid version identifier" in {
      val message = getValidatedLocalMessage(s"""{
                                                |    "type" : ["Corporate Body"],
                                                |    "version-timestamp" : "latest",
                                                |    "authority-file" : false,
                                                |    "depository" : false
                                                |}""".stripMargin)
      val result = listAgentSummaryService.validateRequest(message)
      result mustBe Valid(ListAgentSummary(List(CorporateBody), Some("latest"), Some(false), Some(false)))
    }
  }

  private def getExpectedAgentSummaries: String = s"""[
  {
    "type" : "Person",
    "identifier" : "http://cat.nationalarchives.gov.uk/agent.48N",
    "current-description" : "http://cat.nationalarchives.gov.uk/agent.48N.1",
    "description" : [
      {
        "identifier" : "http://cat.nationalarchives.gov.uk/agent.48N.1",
        "label" : "Baden-Powell",
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1889",
        "date-to" : "1977"
      }
    ]
  },
  {
    "type" : "Person",
    "identifier" : "http://cat.nationalarchives.gov.uk/agent.46F",
    "current-description" : "http://cat.nationalarchives.gov.uk/agent.46F.1",
    "description" : [
      {
        "identifier" : "http://cat.nationalarchives.gov.uk/agent.46F.1",
        "label" : "Fawkes, Guy",
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1570",
        "date-to" : "1606"
      }
    ]
  },
  {
    "type" : "Corporate Body",
    "identifier" : "http://cat.nationalarchives.gov.uk/agent.92W",
    "current-description" : "http://cat.nationalarchives.gov.uk/agent.92W.1",
    "description" : [
      {
        "identifier" : "http://cat.nationalarchives.gov.uk/agent.92W.1",
        "label" : "Joint Milk Quality Committee",
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "1948",
        "date-to" : "1948"
      }
    ]
  },
  {
    "type" : "Corporate Body",
    "identifier" : "http://cat.nationalarchives.gov.uk/agent.8R6",
    "current-description" : "http://cat.nationalarchives.gov.uk/agent.8R6.1",
    "description" : [
      {
        "identifier" : "http://cat.nationalarchives.gov.uk/agent.8R6.1",
        "label" : "Queen Anne's Bounty",
        "depository" : false,
        "version-timestamp" : "2022-06-22T02:00:00-0500"
      }
    ]
  },
  {
    "type" : "Corporate Body",
    "identifier" : "http://cat.nationalarchives.gov.uk/agent.S7",
    "current-description" : "http://cat.nationalarchives.gov.uk/agent.S7.1",
    "description" : [
      {
        "identifier" : "http://cat.nationalarchives.gov.uk/agent.S7.1",
        "label" : "The National Archives, Kew",
        "depository" : true,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2003"
      }
    ]
  }
]""".stripMargin

  private def getExpectedPlaceOfDepositSummaries: String = s"""[
  {
    "type" : "Corporate Body",
    "identifier" : "http://cat.nationalarchives.gov.uk/agent.S7",
    "current-description" : "http://cat.nationalarchives.gov.uk/agent.S7.1",
    "description" : [
      {
        "identifier" : "http://cat.nationalarchives.gov.uk/agent.S7.1",
        "label" : "The National Archives, Kew",
        "depository" : true,
        "version-timestamp" : "2022-06-22T02:00:00-0500",
        "date-from" : "2003"
      }
    ]
  }
]""".stripMargin

}
