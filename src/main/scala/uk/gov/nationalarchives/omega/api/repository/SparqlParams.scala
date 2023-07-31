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
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.AgentTypeMapper

case class SparqlParams(
  booleans: Map[String, Boolean] = Map.empty,
  uris: Map[String, String] = Map.empty,
  dateTimes: Map[String, String] = Map.empty,
  values: Map[String, List[Resource]] = Map.empty,
  queryExtension: Option[String] = None
)
object SparqlParams extends AgentTypeMapper {
  def from(listAgentSummary: ListAgentSummary): SparqlParams = {
    val valuesMap = getValuesMap(listAgentSummary)
    val uriMap = getUriMap(listAgentSummary)
    val booleanMap = getBooleanMap(listAgentSummary)
    val dateTimeMap = getDateTimeMap(listAgentSummary)
    val queryExtension = getQueryExtension(listAgentSummary)
    SparqlParams(booleanMap, uriMap, dateTimeMap, valuesMap, queryExtension)
  }

  private def getValuesMap(listAgentSummary: ListAgentSummary): Map[String, List[Resource]] = {
    val agentTypeUris = listAgentSummary.agentTypes.getOrElse(getAllAgentTypes).map(getUriFromAgentType)
    val agentTypeResources = agentTypeUris.map(ResourceFactory.createResource)
    Map("agentTypeValuesParam" -> agentTypeResources)
  }

  private def getQueryExtension(listAgentSummary: ListAgentSummary): Option[String] =
    listAgentSummary.versionTimestamp match {
      case Some("latest") | None => Some("ORDER BY DESC(?generatedAtParam) LIMIT 1")
      case Some("all")           => Some("ORDER BY DESC(?generatedAtParam)")
      case _                     => None
    }

  private def getUriMap(listAgentSummary: ListAgentSummary): Map[String, String] = {
    val map1 = listAgentSummary.depository match {
      case Some(true) => Map("predicateParam1" -> s"${BaseURL.todo}/is-place-of-deposit")
      case _          => Map.empty[String, String]
    }
    val map2 = listAgentSummary.authorityFile match {
      case Some(true) =>
        Map("predicateParam2" -> s"${BaseURL.dct}/type", "objectParam2" -> s"${BaseURL.cat}/authority-file")
      case _ => Map.empty[String, String]
    }
    map1 ++ map2
  }

  private def getBooleanMap(listAgentSummary: ListAgentSummary): Map[String, Boolean] =
    listAgentSummary.depository match {
      case Some(true) => Map("objectParam1" -> true)
      case _          => Map.empty
    }

  private def getDateTimeMap(listAgentSummary: ListAgentSummary): Map[String, String] =
    listAgentSummary.versionTimestamp match {
      case Some("all") | Some("latest") | None => Map.empty
      case Some(dateTimeValue) =>
        Map("generatedAtParam" -> dateTimeValue)
    }
}
