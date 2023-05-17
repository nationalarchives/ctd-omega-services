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

import cats.effect.{ IO, Resource }
import jms4s.config.QueueName
import jms4s.{ JmsAcknowledgerConsumer, JmsClient, JmsProducer }
import jms4s.sqs.simpleQueueService
import jms4s.sqs.simpleQueueService.{ Config, Credentials, DirectAddress, HTTP }
import org.typelevel.log4cats.Logger
import uk.gov.nationalarchives.omega.api.common.AppLogger
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

class JmsConnector(serviceConfig: ServiceConfig) extends AppLogger {

  /** Binds together a JMS producer and consumer
    * @param inputQueue
    *   the JMS queue the consumer listens to
    * @return
    */
  def getJmsProducerAndConsumer(inputQueue: QueueName): Resource[IO, (JmsProducer[IO], JmsAcknowledgerConsumer[IO])] =
    for {
      jmsClient <- createJmsClient()
      jmsProducerAndConsumer <-
        Resource.both(
          createJmsProducer(jmsClient)(serviceConfig.maxProducers),
          createJmsInputQueueConsumer(jmsClient)(inputQueue, serviceConfig.maxConsumers, 100.millis)
        )
    } yield jmsProducerAndConsumer

  def createJmsClient()(implicit L: Logger[IO]): Resource[IO, JmsClient[IO]] =
    simpleQueueService.makeJmsClient[IO](
      Config(
        endpoint = simpleQueueService.Endpoint(Some(DirectAddress(HTTP, "localhost", Some(9324))), "elasticmq"),
        credentials = Some(Credentials("x", "x")),
        clientId = simpleQueueService.ClientId("ctd-omega-services"),
        None
      )
    )

  def createJmsProducer(client: JmsClient[IO])(concurrencyLevel: Int): Resource[IO, JmsProducer[IO]] =
    client.createProducer(concurrencyLevel)

  private def createJmsInputQueueConsumer(client: JmsClient[IO])(
    queueName: QueueName,
    concurrencyLevel: Int,
    pollingInterval: FiniteDuration
  ): Resource[IO, JmsAcknowledgerConsumer[IO]] =
    client.createAcknowledgerConsumer(queueName, concurrencyLevel, pollingInterval)

}
