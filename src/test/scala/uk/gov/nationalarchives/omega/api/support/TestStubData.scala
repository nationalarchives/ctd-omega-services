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

import uk.gov.nationalarchives.omega.api.messages.{ AgentType, StubData }
import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentDescription, AgentSummary, LegalStatus }

class TestStubData extends StubData {

  override def getLegalStatuses(): List[LegalStatus] = List(
    LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/public-record"), "Public Record"),
    LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/non-public-record"), "Non-Public Record")
  )

  override def getAgentSummaries(): List[AgentSummary] = List(
    AgentSummary(
      AgentType.Person,
      "48N",
      "current",
      AgentDescription("48N", "Baden-Powell", "2022-06-22T02:00:00-0500", None, None, Some("1889"), Some("1977"))
    ),
    AgentSummary(
      AgentType.Person,
      "46F",
      "current description",
      AgentDescription("46F", "Fawkes, Guy",  "2022-06-22T02:00:00-0500", None, None, Some("1570"), Some("1606"))
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "92W",
      "current description",
      AgentDescription(
          "92W",
          "Joint Milk Quality Committee",
          "2022-06-22T02:00:00-0500",
          Some(false),
          Some(false),
          Some("1948"),
          Some("1948")
        )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8R6",
      "current description",
      AgentDescription("8R6", "Queen Anne's Bounty", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    )
  )
}
