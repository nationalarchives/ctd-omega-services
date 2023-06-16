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

import org.apache.jena.query.{ Query, QueryFactory }
import uk.gov.nationalarchives.omega.api.models.LegalStatus

import java.net.URL
import scala.util.{ Failure, Try }

trait AbstractRepository {

  def getLegalStatusSummaries: Try[List[LegalStatus]]

  protected def getQueryResource(queryResource: String): Try[URL] =
    Try(getClass.getClassLoader.getResource(queryResource))

  protected def getQuery(queryUrl: URL): Try[Query] =
    Try(QueryFactory.read(queryUrl.getPath)).recoverWith { case _: NullPointerException =>
      Failure(new IllegalArgumentException(s"Unable to read query from: $queryUrl"))
    }

}
