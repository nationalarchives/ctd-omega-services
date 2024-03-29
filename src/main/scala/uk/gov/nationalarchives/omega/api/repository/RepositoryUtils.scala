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
import cats.implicits.catsSyntaxMonadError
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType
import org.apache.jena.query.{ ParameterizedSparqlString, Query, QueryFactory, Syntax }
import org.apache.jena.rdf.model.{ Resource, ResourceFactory }
import uk.gov.nationalarchives.omega.api.repository.model.AgentTypeMapper

import javax.xml.datatype.XMLGregorianCalendar
import scala.annotation.tailrec
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{ Try, Using }

/** Some helper methods for working with an RDF triplestore */
trait RepositoryUtils extends AgentTypeMapper {

  /** Attempts to read a SPARQL query from the given file path
    * @param queryResource
    *   the path to the SPARQL query resource to use
    * @return
    *   a Success with the parsed Query object or an error
    */
  def prepareQuery(queryResource: String): IO[Query] =
    for {
      queryText <- IO.fromTry(getQueryText(queryResource))
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
    extendQuery: Boolean = false
  ): IO[Query] =
    for {
      queryText          <- IO.fromTry(getQueryText(queryResource))
      extendedQuery      <- setQueryExtension(queryText, params, extendQuery)
      parameterizedQuery <- parameterizeQuery(extendedQuery, params)
      query              <- IO(parameterizedQuery.asQuery(Syntax.syntaxSPARQL_11))
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

  private def setQueryExtension(query: String, sparqlParams: SparqlParams, extendQuery: Boolean): IO[String] = IO {
    if (extendQuery) {
      query + sparqlParams.queryExtension.getOrElse("")
    } else {
      query
    }
  }

  private def getQuery(queryText: String): IO[Query] =
    IO.blocking(QueryFactory.create(queryText, Syntax.syntaxSPARQL_11)).adaptError { case _: NullPointerException =>
      new IllegalArgumentException(s"Unable to read query from text: $queryText")
    }

  private def getQueryText(queryResource: String): Try[String] =
    Using(Source.fromInputStream(getClass.getResourceAsStream(queryResource))) { resource =>
      resource.getLines().mkString("\n")
    }

  private def parameterizeQuery(queryText: String, params: SparqlParams): IO[ParameterizedSparqlString] = IO {
    val parameterize: ParameterizedSparqlString => ParameterizedSparqlString = Seq(
      setBooleanParams(_, params.booleans),
      setResourceParams(_, params.uris),
      setValueParams(_, params.values),
      setXSDDateTimeParams(_, params.dateTimes),
      setStringParams(_, params.strings)
    ).reduceLeft(_.andThen(_))

    val filteredQueryText = setFilterParams(queryText, params.filters)
    parameterize(new ParameterizedSparqlString(filteredQueryText))
  }

  private def setParams[T](
    query: ParameterizedSparqlString,
    params: Map[String, T],
    set: (ParameterizedSparqlString, T) => String => Unit
  ): ParameterizedSparqlString = {
    params.foreach(param => set(query, param._2)(param._1))
    query
  }

  private def setBooleanParams(
    query: ParameterizedSparqlString,
    booleanParams: Map[String, Boolean]
  ): ParameterizedSparqlString =
    setParams[Boolean](
      query,
      booleanParams,
      (query, booleanParamValue) => query.setLiteral(_: String, booleanParamValue)
    )

  private def setStringParams(
    query: ParameterizedSparqlString,
    stringParams: Map[String, String]
  ): ParameterizedSparqlString =
    setParams[String](
      query,
      stringParams,
      (query, stringParamValue) => query.setLiteral(_: String, stringParamValue)
    )

  private val dateTimeType: XSDDateTimeType = new XSDDateTimeType("dateTime")

  private def setXSDDateTimeParams(
    query: ParameterizedSparqlString,
    dateTimeParams: Map[String, XMLGregorianCalendar]
  ): ParameterizedSparqlString =
    setParams[XMLGregorianCalendar](
      query,
      dateTimeParams,
      (query, dateTimeParamValue) => query.setLiteral(_: String, dateTimeParamValue.toXMLFormat, dateTimeType)
    )

  private def setResourceParams(
    query: ParameterizedSparqlString,
    resourceParams: Map[String, String]
  ): ParameterizedSparqlString =
    setParams[String](query, resourceParams, (query, resourceParamValue) => query.setIri(_: String, resourceParamValue))

  private def setValueParams(
    query: ParameterizedSparqlString,
    valueParams: Map[String, List[Resource]]
  ): ParameterizedSparqlString =
    setParams[List[Resource]](
      query,
      valueParams,
      (query, valueParamValue) => query.setValues(_: String, valueParamValue.asJava)
    )

  private def setFilterParams(queryText: String, filterParams: Map[String, String]): String = {

    @tailrec
    def setParams(filterParams: Map[String, String], query: String): String =
      filterParams match {
        case m: Map[String, String] if m.isEmpty => query
        case param =>
          val queryWithFilter = query.replace(s"?${param.head._1}", param.head._2)
          setParams(filterParams.tail, queryWithFilter)
      }

    setParams(filterParams, queryText)

  }

}
