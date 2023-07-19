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

import org.apache.jena.query.{ ParameterizedSparqlString, Query, QueryFactory, Syntax }
import org.apache.jena.rdf.model.Resource
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatus
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.{ AgentDescriptionEntity, AgentEntity, AgentSummaryEntity }

import scala.annotation.tailrec
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Try, Using }

trait AbstractRepository {

  def getLegalStatusSummaries: Try[List[LegalStatus]]

  def getAgentEntities: Try[List[AgentEntity]]

  def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): Try[List[AgentSummaryEntity]]

  def getAgentDescriptionEntities: Try[List[AgentDescriptionEntity]]

  def getPlaceOfDepositEntities: Try[List[AgentEntity]]

  protected def getQueryText(queryResource: String): Try[String] =
    Using(Source.fromInputStream(getClass.getResourceAsStream(queryResource))) { resource =>
      resource.getLines().mkString("\n")
    }

  protected def getQuery(queryText: String): Try[Query] =
    Try(QueryFactory.create(queryText, Syntax.syntaxSPARQL_11)).recoverWith { case _: NullPointerException =>
      Failure(new IllegalArgumentException(s"Unable to read query from text: $queryText"))
    }

  protected def parameterizeQuery(queryText_0: String, params: SparqlParams): Try[ParameterizedSparqlString] =
    for {
      queryText_1 <- Try(setBooleanParams(queryText_0, params.booleans))
      queryText_2 <- Try(setResourceParams(queryText_1, params.uris))
      queryText_3 <- Try(setValueParams(queryText_2, params.values))
    } yield new ParameterizedSparqlString(queryText_3)

  private def setBooleanParams(queryText: String, booleans: Map[String, Boolean]): String = {

    @tailrec
    def setParams(bools: Map[String, Boolean], query: String): String =
      bools match {
        case m: Map[String, Boolean] if m.isEmpty => query
        case param =>
          val pss = new ParameterizedSparqlString(query)
          pss.setLiteral(param.head._1, param.head._2)
          setParams(bools.tail, pss.toString)
      }
    setParams(booleans, queryText)
  }

  private def setResourceParams(queryText: String, resources: Map[String, String]): String = {

    @tailrec
    def setParams(resMap: Map[String, String], query: String): String =
      resMap match {
        case m: Map[String, String] if m.isEmpty => query
        case param =>
          val pss = new ParameterizedSparqlString(query)
          pss.setIri(param.head._1, param.head._2)
          setParams(resMap.tail, pss.toString)
      }
    setParams(resources, queryText)
  }

  private def setValueParams(queryText: String, values: Map[String, List[Resource]]) = {

    @tailrec
    def setParams(resMap: Map[String, List[Resource]], query: String): String =
      resMap match {
        case m: Map[String, List[Resource]] if m.isEmpty => query
        case param =>
          val pss = new ParameterizedSparqlString(query)
          pss.setValues(param.head._1, param.head._2.asJava)
          setParams(resMap.tail, pss.toString)
      }
    setParams(values, queryText)
  }

}
