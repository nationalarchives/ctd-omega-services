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

import cats.effect.IO
import org.apache.jena.rdf.model.{ Resource, ResourceFactory }
import org.apache.jena.vocabulary.DCTerms
import uk.gov.nationalarchives.omega.api.messages.request.{ ListAgentSummary, RequestByIdentifier }
import uk.gov.nationalarchives.omega.api.repository.model.AgentTypeMapper
import uk.gov.nationalarchives.omega.api.repository.vocabulary.{ Cat, TODO }

import java.time.ZonedDateTime
import java.util.GregorianCalendar
import javax.xml.datatype.{ DatatypeFactory, XMLGregorianCalendar }
import scala.util.Try

case class SparqlParams(
  booleans: Map[String, Boolean] = Map.empty,
  uris: Map[String, String] = Map.empty,
  dateTimes: Map[String, XMLGregorianCalendar] = Map.empty,
  values: Map[String, List[Resource]] = Map.empty,
  filters: Map[String, String] = Map.empty,
  queryExtension: Option[String] = None,
  strings: Map[String, String] = Map.empty
)
object SparqlParams extends AgentTypeMapper {

  def from(listAgentSummary: ListAgentSummary): IO[SparqlParams] =
    for {
      valuesMap      <- getValuesMap(listAgentSummary)
      uriMap         <- getUriMap(listAgentSummary)
      booleanMap     <- getBooleanMap(listAgentSummary)
      dateTimeMap    <- getDateTimeMap(listAgentSummary)
      queryExtension <- getQueryExtension(listAgentSummary)
      filterMap      <- getFilterMap(listAgentSummary)
    } yield SparqlParams(booleanMap, uriMap, dateTimeMap, valuesMap, filterMap, queryExtension)

  def from(getRecord: RequestByIdentifier): Try[SparqlParams] =
    for {
      uriMap <- Try(getUriMap(getRecord))
    } yield SparqlParams(uris = uriMap)

  private def getValuesMap(listAgentSummary: ListAgentSummary): IO[Map[String, List[Resource]]] = IO {
    val agentTypeUris = listAgentSummary.agentTypes.getOrElse(getAllAgentTypes).map(getUriFromAgentType)
    val agentTypeResources = agentTypeUris.map(ResourceFactory.createResource)
    Map("agentTypeValuesParam" -> agentTypeResources)
  }

  private def getQueryExtension(listAgentSummary: ListAgentSummary): IO[Option[String]] = IO {
    listAgentSummary.versionTimestamp match {
      case Some("latest") | None => Some("ORDER BY DESC(?versionTimestamp) LIMIT 1")
      case Some("all")           => Some("ORDER BY DESC(?versionTimestamp)")
      case _                     => None
    }
  }

  private def getUriMap(listAgentSummary: ListAgentSummary): IO[Map[String, String]] = IO {
    val map1 = listAgentSummary.depository match {
      case Some(true) => Map("predicateParam1" -> TODO.isPlaceOfDeposit)
      case _          => Map.empty[String, String]
    }
    val map2 = listAgentSummary.authorityFile match {
      case Some(true) =>
        Map("predicateParam2" -> DCTerms.`type`.getURI, "objectParam2" -> Cat.authorityFile)
      case _ => Map.empty[String, String]
    }
    map1 ++ map2
  }

  private def getUriMap(requestByIdentifier: RequestByIdentifier): Map[String, String] =
    Map("recordConceptUri" -> requestByIdentifier.identifier)

  private def getBooleanMap(listAgentSummary: ListAgentSummary): IO[Map[String, Boolean]] = IO {
    listAgentSummary.depository match {
      case Some(true) => Map("objectParam1" -> true)
      case _          => Map.empty
    }
  }

  private def getDateTimeMap(listAgentSummary: ListAgentSummary): IO[Map[String, XMLGregorianCalendar]] = IO {
    listAgentSummary.versionTimestamp match {
      case Some("all") | Some("latest") | None => Map.empty
      case Some(dateTimeValue) =>
        val date = ZonedDateTime.parse(dateTimeValue)
        val calendar = GregorianCalendar.from(date)
        Map("generatedAtParam" -> DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar))
    }
  }

  private def getFilterMap(listAgentSummary: ListAgentSummary): IO[Map[String, String]] = IO {
    listAgentSummary.versionTimestamp match {
      case Some("all") | Some("latest") | None => Map("filterParam" -> "")
      case Some(dateTimeValue) => Map("filterParam" -> s"FILTER(?versionTimestamp >= xsd:dateTime(\"$dateTimeValue\"))")
    }
  }

}
