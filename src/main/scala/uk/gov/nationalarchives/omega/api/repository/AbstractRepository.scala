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

import org.apache.jena.query.{ Query, QueryFactory, Syntax }
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatus

import scala.io.Source
import scala.util.{ Failure, Try, Using }

trait AbstractRepository {

  def getLegalStatusSummaries: Try[List[LegalStatus]]

  protected def getQueryText(queryResource: String): Try[String] =
    Using(Source.fromInputStream(getClass.getResourceAsStream(queryResource))) { resource =>
      resource.getLines().mkString("\n")
    }

  protected def getQuery(queryText: String): Try[Query] =
    Try(QueryFactory.create(queryText, Syntax.syntaxSPARQL_11)).recoverWith { case _: NullPointerException =>
      Failure(new IllegalArgumentException(s"Unable to read query from text: $queryText"))
    }

}
