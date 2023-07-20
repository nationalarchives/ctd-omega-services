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

import org.apache.jena.rdf.model.{Resource, ResourceFactory}
import uk.gov.nationalarchives.omega.api.messages.AgentType
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.AgentEntity.getUriFromAgentType

case class SparqlParams(
  booleans: Map[String, Boolean] = Map.empty,
  uris: Map[String, String] = Map.empty,
  values: Map[String, List[Resource]] = Map.empty
)
object SparqlParams {
  def from(listAgentSummary: ListAgentSummary): SparqlParams = {
    val valuesMap = getValuesMap(listAgentSummary.agentType)
    val uriMap = getUriMap(listAgentSummary)
    val booleanMap = getBooleanMap(listAgentSummary)
    SparqlParams(booleanMap,uriMap,valuesMap)
  }

  private def getValuesMap(agentTypes: List[AgentType]): Map[String, List[Resource]] = {
    if(agentTypes.nonEmpty) {
      val agentTypeUris = agentTypes.map(getUriFromAgentType)
      val agentTypeResources = agentTypeUris.map(ResourceFactory.createResource)
      Map("agentTypeValues" -> agentTypeResources)
    } else {
      Map.empty
    }
  }

  def getUriMap(listAgentSummary: ListAgentSummary): Map[String, String] = {
    //listAgentSummary.depository // todo:isplace of deposit
    //listAgentSummary.authorityFile // dct:type + cat:authority-file]
    Map.empty
  }

  private def getBooleanMap(listAgentSummary: ListAgentSummary): Map[String, Boolean] = {
    Map.empty
  }
}
