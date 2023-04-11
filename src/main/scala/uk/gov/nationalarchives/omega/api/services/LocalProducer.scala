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

import cats.data.NonEmptyChain
import cats.effect.IO
import jms4s.JmsProducer
import jms4s.config.QueueName
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidationError, TextIsNonEmptyCharacters }
import uk.gov.nationalarchives.omega.api.services.LocalMessage._

trait LocalProducer {
  def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit]

  def sendWhenGenericRequestIsInvalid(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit]

  def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    errors: NonEmptyChain[BusinessRequestValidationError]
  ): IO[Unit]

  def localMessageValidationErrorsToReplyMessage(
    localMessageValidationErrors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): String = localMessageValidationErrors
    .map(localMessageValidationError => toErrorMessage(localMessageValidationError))
    .filter(_.isDefined)
    .map(_.get)
    .toList
    .mkString(";")

  def businessRequestValidationErrorToReplyMessage(
    errors: NonEmptyChain[BusinessRequestValidationError]
  ): String = errors
    .map(error => toErrorMessage(error))
    .filter(_.isDefined)
    .map(_.get)
    .toList
    .mkString(";")

  private def toErrorMessage(genericRequestValidationError: LocalMessageValidationError): Option[String] =
    genericRequestValidationError match {
      case MissingJMSMessageID    => Some("Missing JMSMessageID")
      case InvalidJMSMessageID    => Some("Invalid JMSMessageID")
      case MissingServiceID       => Some("Missing OMGMessageTypeID")
      case InvalidServiceID       => Some("Invalid OMGMessageTypeID")
      case MissingApplicationID   => Some("Missing OMGApplicationID")
      case InvalidApplicationID   => Some("Invalid OMGApplicationID")
      case MissingJMSTimestamp    => Some("Missing JMSTimestamp")
      case MissingMessageFormat   => Some("Missing OMGMessageFormat")
      case InvalidMessageFormat   => Some("Invalid OMGMessageFormat")
      case MissingAuthToken       => Some("Missing OMGToken")
      case InvalidAuthToken       => Some("Invalid OMGToken")
      case MissingResponseAddress => Some("Missing OMGResponseAddress")
      case InvalidResponseAddress => Some("Invalid OMGResponseAddress")
    }

  private def toErrorMessage(businessRequestValidationError: BusinessRequestValidationError): Option[String] =
    businessRequestValidationError match {
      case TextIsNonEmptyCharacters(message, _) => Some(s"Message text is blank: $message")
    }
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

  override def sendWhenGenericRequestIsInvalid(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] =
    localMessage.correlationId
      .filter(_.trim.nonEmpty)
      .map { correlationId =>
        jmsProducer.send { mf =>
          val msg = mf.makeTextMessage(localMessageValidationErrorsToReplyMessage(errors))
          msg.map { m =>
            m.setJMSCorrelationId(correlationId)
            (m, outputQueue)
          }
        } *> IO.unit
      }
      .getOrElse(IO.pure {})

  override def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    businessRequestValidationErrors: NonEmptyChain[BusinessRequestValidationError]
  ): IO[Unit] =
    localMessage.correlationId
      .filter(_.trim.nonEmpty)
      .map { correlationId =>
        jmsProducer.send { mf =>
          val msg = mf.makeTextMessage(businessRequestValidationErrorToReplyMessage(businessRequestValidationErrors))
          msg.map { m =>
            m.setJMSCorrelationId(correlationId)
            (m, outputQueue)
          }
        } *> IO.unit
      }
      .getOrElse(IO.pure {})
}
