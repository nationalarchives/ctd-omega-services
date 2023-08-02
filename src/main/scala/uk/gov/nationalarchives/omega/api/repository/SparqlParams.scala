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

import java.time.ZonedDateTime
import java.util.{ Date, GregorianCalendar }
import javax.xml.datatype.{ DatatypeFactory, XMLGregorianCalendar }
import scala.util.Try

case class SparqlParams(
  booleans: Map[String, Boolean] = Map.empty,
  uris: Map[String, String] = Map.empty,
  dateTimes: Map[String, XMLGregorianCalendar] = Map.empty,
  values: Map[String, List[Resource]] = Map.empty,
  filters: Map[String, String] = Map.empty,
  queryExtension: Option[String] = None
)
object SparqlParams extends AgentTypeMapper {

  def from(listAgentSummary: ListAgentSummary): Try[SparqlParams] =
    for {
      valuesMap      <- Try(getValuesMap(listAgentSummary))
      uriMap         <- Try(getUriMap(listAgentSummary))
      booleanMap     <- Try(getBooleanMap(listAgentSummary))
      dateTimeMap    <- getDateTimeMap(listAgentSummary)
      queryExtension <- Try(getQueryExtension(listAgentSummary))
      filterMap      <- Try(getFilterMap(listAgentSummary))
    } yield SparqlParams(booleanMap, uriMap, dateTimeMap, valuesMap, filterMap, queryExtension)

  private def getValuesMap(listAgentSummary: ListAgentSummary): Map[String, List[Resource]] = {
    val agentTypeUris = listAgentSummary.agentTypes.getOrElse(getAllAgentTypes).map(getUriFromAgentType)
    val agentTypeResources = agentTypeUris.map(ResourceFactory.createResource)
    Map("agentTypeValuesParam" -> agentTypeResources)
  }

  private def getQueryExtension(listAgentSummary: ListAgentSummary): Option[String] =
    listAgentSummary.versionTimestamp match {
      case Some("latest") | None => Some("ORDER BY DESC(?versionTimestamp) LIMIT 1")
      case Some("all")           => Some("ORDER BY DESC(?versionTimestamp)")
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

  private def getDateTimeMap(listAgentSummary: ListAgentSummary): Try[Map[String, XMLGregorianCalendar]] = Try {
    listAgentSummary.versionTimestamp match {
      case Some("all") | Some("latest") | None => Map.empty
      case Some(dateTimeValue) =>
        val date = ZonedDateTime.parse(dateTimeValue)
        val calendar = new GregorianCalendar()
        calendar.setTime(Date.from(date.toInstant))
        Map("generatedAtParam" -> DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar))
    }
  }

  private def getFilterMap(listAgentSummary: ListAgentSummary): Map[String, String] =
    listAgentSummary.versionTimestamp match {
      case Some("all") | Some("latest") | None => Map("filterParam" -> "")
      case Some(dateTimeValue) => Map("filterParam" -> s"FILTER(?versionTimestamp >= xsd:dateTime(\"$dateTimeValue\"))")
    }

}
