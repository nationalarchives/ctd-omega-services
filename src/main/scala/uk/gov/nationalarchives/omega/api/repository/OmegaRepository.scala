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

import org.apache.jena.query.QuerySolution
import org.phenoscape.sparql.FromQuerySolution
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentSummary, LegalStatus }

import scala.util.Try

class OmegaRepository(sparqlConnector: SparqlEndpointConnector) extends AbstractRepository {

  private val sparqlResourceDir = "sparql"
  private val getLegalStatusSummarySparqlResource = s"/$sparqlResourceDir/select-legal-status-summaries.rq"
  private val getAgentSummariesSparqlResource = s"/$sparqlResourceDir/select-agent-summaries.rq"
  private val getPlaceOfDepositSummariesSparqlResource = s"/$sparqlResourceDir/select-place-of-deposit-summaries.rq"

  override def getLegalStatusSummaries: Try[List[LegalStatus]] =
    processQuery[LegalStatus](getLegalStatusSummarySparqlResource, implicitly[FromQuerySolution[LegalStatus]])

  override def getAgentSummaries: Try[List[AgentSummary]] =
    processQuery[AgentSummary](getAgentSummariesSparqlResource, implicitly[FromQuerySolution[AgentSummary]])

  override def getPlaceOfDepositSummaries: Try[List[AgentSummary]] =
    processQuery[AgentSummary](getPlaceOfDepositSummariesSparqlResource, implicitly[FromQuerySolution[AgentSummary]])

  def processQuery[A](queryResource: String, queryDecoder: FromQuerySolution[A]): Try[List[A]] =
    for {
      queryText <- getQueryText(queryResource)
      query     <- getQuery(queryText)
      result    <- sparqlConnector.execute(query, queryDecoder)
    } yield result

  implicit object BooleanFromQuerySolution extends FromQuerySolution[Boolean] {
    def fromQuerySolution(qs: QuerySolution, variablePath: String = ""): Try[Boolean] =
      getLiteral(qs, variablePath).map(_.getBoolean)
  }
}
