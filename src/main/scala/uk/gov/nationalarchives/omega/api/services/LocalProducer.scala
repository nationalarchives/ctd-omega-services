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

package uk.gov.nationalarchives.omega.api.services

import cats.effect.IO
import jms4s.JmsProducer
import jms4s.config.QueueName

trait LocalProducer {
  def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit]
}

/** In JMS terms a producer can have one or many destinations - in this implementation we have one destination, if we
  * want many destinations we need to modify the send method to pass the destination with each call
  */
class LocalProducerImpl(val jmsProducer: JmsProducer[IO], val outputQueue: QueueName) extends LocalProducer {

  /** Send the given reply message to the output queue
    * @param replyMessage
    *   \- the message
    * @param requestMessage
    *   the request message (needed for correlation ID)
    * @return
    */
  def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit] =
    jmsProducer.send { mf =>
      val msg = mf.makeTextMessage(replyMessage)
      msg.map { m =>
        m.setJMSCorrelationId(requestMessage.correlationId)
        (m, outputQueue)
      }
    } *> IO.unit

}
