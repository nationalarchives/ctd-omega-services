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

package uk.gov.nationalarchives.omega.api.connectors

import org.apache.jena.query.{ ARQ, Query, QueryExecutionFactory }
import org.apache.jena.sparql.exec.http.{ QueryExecutionHTTP, QueryExecutionHTTPBuilder, QuerySendMode }
import org.phenoscape.sparql.FromQuerySolution
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig

import scala.jdk.CollectionConverters._
import scala.util.{ Try, Using }

class SparqlEndpointConnector(config: ServiceConfig) {

  /** Executes the provided SPARQL query against the configured endpoint and decodes the result with the provided
    * decoder
    * @param query
    *   \- the SPARQL to execute
    * @param queryDecoder
    *   \- a decoder for the result
    * @tparam T
    *   \- the result type
    * @return
    */
  def execute[T](query: Query, queryDecoder: FromQuerySolution[T]): Try[List[T]] =
    Using(QueryExecutionHTTP.service(config.sparqlEndpoint).query(query).sendMode(QuerySendMode.asPost).build()) {
      queryExec =>
        val resultSet = queryExec.execSelect()
        val itResults = resultSet.asScala.toList
        itResults.flatMap { querySolution =>
          queryDecoder.fromQuerySolution(querySolution).toOption
        }
    }

}
