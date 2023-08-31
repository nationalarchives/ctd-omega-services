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

import java.time.ZonedDateTime
import java.util.{ Date, GregorianCalendar }
import javax.xml.datatype.{ DatatypeFactory, XMLGregorianCalendar }
import scala.util.Success

class SparqlParamsSpec extends UnitTest with AgentTypeMapper {

  "SparqlParams must contain" - {
    "all agent type values and a query extension when ListAgentSummary is empty" in {
      val result = SparqlParams.from(ListAgentSummary())
      result mustEqual Success(
        SparqlParams(
          values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
          filters = Map("filterParam" -> ""),
          queryExtension = Some("ORDER BY DESC(?versionTimestamp) LIMIT 1")
        )
      )
    }
    "specific agent type values and a query extension when ListAgentSummary specifies agent types" in {
      val result = SparqlParams.from(ListAgentSummary(agentTypes = Some(List(CorporateBody, Person))))
      result mustEqual Success(
        SparqlParams(
          values = Map("agentTypeValuesParam" -> getAgentTypeResources(List(CorporateBody, Person))),
          filters = Map("filterParam" -> ""),
          queryExtension = Some("ORDER BY DESC(?versionTimestamp) LIMIT 1")
        )
      )
    }
    "all agent type values and a boolean when ListAgentSummary specifies depository" in {
      val result = SparqlParams.from(ListAgentSummary(depository = Some(true)))
      result mustEqual Success(
        SparqlParams(
          booleans = Map("objectParam1" -> true),
          uris = Map("predicateParam1" -> "http://TODO/is-place-of-deposit"),
          values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
          filters = Map("filterParam" -> ""),
          queryExtension = Some("ORDER BY DESC(?versionTimestamp) LIMIT 1")
        )
      )
    }
    "all agent type values and no limit on query extension when ListAgentSummary specifies version timestamp 'all'" in {
      val result = SparqlParams.from(ListAgentSummary(versionTimestamp = Some("all")))
      result mustEqual Success(
        SparqlParams(
          values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
          filters = Map("filterParam" -> ""),
          queryExtension = Some("ORDER BY DESC(?versionTimestamp)")
        )
      )
    }
    "all agent type values and no query extension when ListAgentSummary specifies a version timestamp date" in {
      val timestamp = "2023-01-25T14:14:49.601Z"
      val result = SparqlParams.from(ListAgentSummary(versionTimestamp = Some(timestamp)))
      result mustEqual Success(
        SparqlParams(
          dateTimes = Map("generatedAtParam" -> getGregorianDateTime(timestamp)),
          values = Map("agentTypeValuesParam" -> getAgentTypeResources(getAllAgentTypes)),
          filters = Map("filterParam" -> "FILTER(?versionTimestamp >= xsd:dateTime(\"2023-01-25T14:14:49.601Z\"))"),
          queryExtension = None
        )
      )
    }
  }

  private def getAgentTypeResources(agentTypes: List[AgentType]): List[Resource] =
    agentTypes.map(agentType => ResourceFactory.createResource(getUriFromAgentType(agentType)))

  private def getGregorianDateTime(dateTimeValue: String): XMLGregorianCalendar = {
    val date = ZonedDateTime.parse(dateTimeValue)
    val calendar = new GregorianCalendar()
    calendar.setTime(Date.from(date.toInstant))
    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
  }

}
