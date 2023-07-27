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

package uk.gov.nationalarchives.omega.api.models

import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import io.circe.parser.decode
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CorporateBody, Person }
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary

class ListAgentSummarySpec extends AnyFreeSpec with Matchers {

  "ListAgentSummary" - {
    "must decode a request with multiple types" in {
      val agentRequest =
        s"""{
           |    "type" : ["Corporate Body","Person"]
           |}""".stripMargin
      decode[ListAgentSummary](agentRequest) mustBe Right(value =
        ListAgentSummary(agentTypes = Some(List(CorporateBody, Person)), None, None, None)
      )
    }
    "must decode a request with depository" in {
      val agentRequest =
        """
          |{
          |  "type": ["Corporate Body"],
          |  "depository": true
          |}
          |""".stripMargin
      decode[ListAgentSummary](agentRequest) mustBe Right(value =
        ListAgentSummary(agentTypes = Some(List(CorporateBody)), None, depository = Some(true), None)
      )
    }
  }

}
