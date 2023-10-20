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

package uk.gov.nationalarchives.omega.api.connectors.httpclient

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.neptune.auth.NeptuneApacheHttpSigV4Signer
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpException, HttpRequest, HttpRequestInterceptor}

import scala.util.{Failure, Try}

/**
 * An HttpRequestIntercepted for Apache Commons Http Client
 * that adds V4 Signature Signing to the HTTP Request by
 * authentication with AWS IAM.
 *
 * See <a href="https://docs.aws.amazon.com/neptune/latest/userguide/iam-auth-connecting-sparql-java.html">Connecting to Neptune Using Java and SPARQL with Signature Version 4 Signing (RDF4J and Jena)</a>.
 *
 * @param awsRegionName The AWS Region Name for IAM authentication.
 */
class V4SigningHttpRequestInterceptor(awsRegionName: String) extends HttpRequestInterceptor {

  private val awsCredentialsProvider = new DefaultAWSCredentialsProviderChain()
  private val v4Signer = new NeptuneApacheHttpSigV4Signer(awsRegionName, awsCredentialsProvider)

  @throws[HttpException]
  override def process(request: HttpRequest, context: HttpContext): Unit = {
    request match {
      case httpUriRequest: HttpUriRequest =>
        Try(v4Signer.signRequest(httpUriRequest)).recoverWith(t => Failure(new HttpException("Problem signing the request: ", t)))
      case _ =>
        throw new HttpException("Not an HttpUriRequest"); // this should never happen
    }
  }
}
