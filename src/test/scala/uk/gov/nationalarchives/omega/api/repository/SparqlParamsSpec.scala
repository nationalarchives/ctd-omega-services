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

package uk.gov.nationalarchives.omega.api.repository

import org.apache.jena.rdf.model.{ Resource, ResourceFactory }
import uk.gov.nationalarchives.omega.api.messages.AgentType
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CorporateBody, Person }
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.AgentTypeMapper
import uk.gov.nationalarchives.omega.api.support.UnitTest

class SparqlParamsSpec extends UnitTest with AgentTypeMapper {

  "SparqlParams must contain" - {
    "all agent type values and a query extension when ListAgentSummary is empty" in {
      val result = SparqlParams.from(ListAgentSummary())
      result mustEqual SparqlParams(
        values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
        queryExtension = Some("ORDER BY DESC(?generatedAtParam) LIMIT 1")
      )
    }
    "specific agent type values and a query extension when ListAgentSummary specifies agent types" in {
      val result = SparqlParams.from(ListAgentSummary(agentTypes = Some(List(CorporateBody, Person))))
      result mustEqual SparqlParams(
        values = Map("agentTypeValuesParam" -> getAgentTypeResources(List(CorporateBody, Person))),
        queryExtension = Some("ORDER BY DESC(?generatedAtParam) LIMIT 1")
      )
    }
    "all agent type values and a boolean when ListAgentSummary specifies depository" in {
      val result = SparqlParams.from(ListAgentSummary(depository = Some(true)))
      result mustEqual SparqlParams(
        booleans = Map("objectParam1" -> true),
        uris = Map("predicateParam1" -> "http://TODO/is-place-of-deposit"),
        values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
        queryExtension = Some("ORDER BY DESC(?generatedAtParam) LIMIT 1")
      )
    }
    "all agent type values no limit on query extension when ListAgentSummary specifies version timestamp 'all'" in {
      val result = SparqlParams.from(ListAgentSummary(versionTimestamp = Some("all")))
      result mustEqual SparqlParams(
        values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
        queryExtension = Some("ORDER BY DESC(?generatedAtParam)")
      )
    }
  }

  private def getAgentTypeResources(agentTypes: List[AgentType]): List[Resource] =
    agentTypes.map(agentType => ResourceFactory.createResource(getUriFromAgentType(agentType)))

}
