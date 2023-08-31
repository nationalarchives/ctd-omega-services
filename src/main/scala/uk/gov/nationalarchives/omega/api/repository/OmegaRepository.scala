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

import org.apache.jena.ext.xerces.util.URI
import org.apache.jena.query.{ Query, QuerySolution }
import org.phenoscape.sparql.FromQuerySolution
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.{ AgentConceptEntity, AgentDescriptionEntity, LegalStatusEntity }

import java.time.ZonedDateTime
import scala.util.Try

class OmegaRepository(sparqlConnector: SparqlEndpointConnector) extends AbstractRepository with RepositoryUtils {

  private val sparqlResourceDir = "sparql"
  private val selectLegalStatusSummarySparqlResource = s"/$sparqlResourceDir/select-legal-status-summaries.rq"
  private val getAgentSummariesSparqlResource = s"/$sparqlResourceDir/get-agent-concepts.rq"
  private val getAgentDescriptionsSparqlResource = s"/$sparqlResourceDir/get-agent-descriptions.rq"

  implicit object BooleanFromQuerySolution extends FromQuerySolution[Boolean] {
    def fromQuerySolution(qs: QuerySolution, variablePath: String = ""): Try[Boolean] =
      getLiteral(qs, variablePath).map(_.getBoolean)
  }

  implicit object ZoneDateTimeFromQuerySolution extends FromQuerySolution[ZonedDateTime] {
    def fromQuerySolution(qs: QuerySolution, variablePath: String = ""): Try[ZonedDateTime] =
      getLiteral(qs, variablePath).map(value => ZonedDateTime.parse(value.getString))
  }

  override def getLegalStatusEntities: Try[List[LegalStatusEntity]] =
    for {
      query  <- prepareQuery(selectLegalStatusSummarySparqlResource)
      result <- executeQuery(query, implicitly[FromQuerySolution[LegalStatusEntity]])
    } yield result

  override def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): Try[List[AgentConceptEntity]] =
    for {
      params <- SparqlParams.from(listAgentSummary)
      query  <- prepareParameterizedQuery(getAgentSummariesSparqlResource, params, extendQuery = false)
      result <- executeQuery(query, implicitly[FromQuerySolution[AgentConceptEntity]])
    } yield result

  override def getAgentDescriptionEntities(
    listAgentSummary: ListAgentSummary,
    agentConceptUri: URI
  ): Try[List[AgentDescriptionEntity]] =
    for {
      params <- SparqlParams.from(listAgentSummary)
      query <- prepareParameterizedQuery(
                 getAgentDescriptionsSparqlResource,
                 params.copy(uris = params.uris ++ Map("conceptIdParam" -> agentConceptUri.toString)),
                 extendQuery = true
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[AgentDescriptionEntity]])
    } yield result

  private def executeQuery[A](query: Query, queryDecoder: FromQuerySolution[A]): Try[List[A]] =
    sparqlConnector.execute(query, queryDecoder)

}
