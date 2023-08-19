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

import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType
import org.apache.jena.query.{ ParameterizedSparqlString, Query, QueryFactory, Syntax }
import org.apache.jena.rdf.model.{ Resource, ResourceFactory }
import uk.gov.nationalarchives.omega.api.repository.model.AgentTypeMapper

import javax.xml.datatype.XMLGregorianCalendar
import scala.annotation.tailrec
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Try, Using }

/** Some helper methods for working with an RDF triplestore */
trait RepositoryUtils extends AgentTypeMapper {

  /** Attempts to read a SPARQL query from the given file path
    * @param queryResource
    *   the path to the SPARQL query resource to use
    * @return
    *   a Success with the parsed Query object or an error
    */
  def prepareQuery(queryResource: String): Try[Query] =
    for {
      queryText <- getQueryText(queryResource)
      query     <- getQuery(queryText)
    } yield query

  /** Attempts to read a SPARQL query from the given file path and parameterize it with the given parameters.
    * @param queryResource
    *   the path of the SPARQL query resource to use
    * @param params
    *   the parameters to apply to the SPARQL query
    * @param extendQuery
    *   Boolean to indicate whether to apply any query extension in the params
    * @return
    *   a Success with the parsed and parameterized Query object or an error
    */
  def prepareParameterizedQuery(
    queryResource: String,
    params: SparqlParams,
    extendQuery: Boolean
  ): Try[Query] =
    for {
      queryText          <- getQueryText(queryResource)
      extendedQuery      <- setQueryExtension(queryText, params, extendQuery)
      parameterizedQuery <- parameterizeQuery(extendedQuery, params)
      query              <- Try(parameterizedQuery.asQuery(Syntax.syntaxSPARQL_11))
    } yield query

  /** Convenience method to create a Jena Resource from a given base URL and local name
    * @param baseUrl
    *   the base URL to use
    * @param localName
    *   the local name to use
    * @return
    *   the created Resource object
    */
  def createResource(baseUrl: String, localName: String): Resource =
    ResourceFactory.createResource(s"$baseUrl/$localName")

  private def setQueryExtension(query: String, sparqlParams: SparqlParams, extendQuery: Boolean): Try[String] =
    if (extendQuery) {
      Try(query + sparqlParams.queryExtension.getOrElse(""))
    } else {
      Try(query)
    }

  private def getQuery(queryText: String): Try[Query] =
    Try(QueryFactory.create(queryText, Syntax.syntaxSPARQL_11)).recoverWith { case _: NullPointerException =>
      Failure(new IllegalArgumentException(s"Unable to read query from text: $queryText"))
    }

  private def getQueryText(queryResource: String): Try[String] =
    Using(Source.fromInputStream(getClass.getResourceAsStream(queryResource))) { resource =>
      resource.getLines().mkString("\n")
    }

  private def parameterizeQuery(queryText_0: String, params: SparqlParams): Try[ParameterizedSparqlString] =
    for {
      queryText_1 <- Try(setBooleanParams(queryText_0, params.booleans))
      queryText_2 <- Try(setResourceParams(queryText_1, params.uris))
      queryText_3 <- Try(setValueParams(queryText_2, params.values))
      queryText_4 <- Try(setXSDDateTimeParams(queryText_3, params.dateTimes))
    } yield new ParameterizedSparqlString(queryText_4)

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

  private def setXSDDateTimeParams(queryText: String, dateTimes: Map[String, XMLGregorianCalendar]): String = {

    @tailrec
    def setParams(dates: Map[String, XMLGregorianCalendar], query: String): String =
      dates match {
        case m: Map[String, XMLGregorianCalendar] if m.isEmpty => query
        case param =>
          val pss = new ParameterizedSparqlString(query)
          pss.setLiteral(param.head._1, param.head._2.toXMLFormat, new XSDDateTimeType("dateTime"))
          setParams(dates.tail, pss.toString)
      }

    setParams(dateTimes, queryText)
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