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

package uk.gov.nationalarchives.omega.api.conf

case class ServiceConfig(
  messageStoreDir: String,
  maxConsumers: Int,
  maxProducers: Int,
  maxDispatchers: Int,
  maxLocalQueueSize: Int,
  requestQueue: String,
  sqsJmsBroker: SqsJmsBroker,
  sparqlRemote: SparqlRemote
)

case class SqsJmsBroker(awsRegion: String, endpoint: Option[SqsJmsBrokerEndpoint])

case class SqsJmsBrokerEndpoint(
  tls: Boolean,
  host: Option[String],
  port: Option[Int],
  authentication: Option[AwsCredentialsAuthentication]
)

case class AwsCredentialsAuthentication(accessKey: String, secretKey: String)

case class SparqlRemote(
  uri: String,
  queryEndpoint: Option[String] = None,
  updateEndpoint: Option[String] = None,
  authentication: Option[SparqlRemoteAuthentication] = None
)

case class SparqlRemoteAuthentication(iam: IamRegionAuthentication)

case class IamRegionAuthentication(awsRegion: String)
