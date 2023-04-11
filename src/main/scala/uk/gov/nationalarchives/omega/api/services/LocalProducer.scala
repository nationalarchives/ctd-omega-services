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
import io.circe.Json
import io.circe.syntax.EncoderOps
import jms4s.JmsProducer
import jms4s.config.QueueName
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidationError, BusinessServiceError, TextIsNonEmptyCharacters }
import uk.gov.nationalarchives.omega.api.common.ErrorCode.{ BLAN001, INVA001, INVA002, INVA003, INVA005, INVA006, INVA007, MISS001, MISS002, MISS003, MISS004, MISS005, MISS006, MISS007 }
import uk.gov.nationalarchives.omega.api.common.{ ErrorCode, JsonError }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage._
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, MessageProperties, OutgoingMessageType, ValidatedLocalMessage }

trait LocalProducer {
  def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit]

  def sendProcessingError(businessServiceError: BusinessServiceError, requestMessage: ValidatedLocalMessage): IO[Unit]

  def sendInvalidMessageFormatError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit]

  def sendUnrecognisedMessageTypeError(localMessage: LocalMessage): IO[Unit]

  def sendInvalidApplicationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit]

  def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    errors: NonEmptyChain[BusinessRequestValidationError]
  ): IO[Unit]

  def localMessageValidationErrorsToReplyMessage(
    localMessageValidationErrors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): Json = localMessageValidationErrors
    .map(localMessageValidationError => toErrorMessage(localMessageValidationError))
    .filter(_.isDefined)
    .map(_.get)
    .toList
    .asJson

  def createProcessingErrorMessage(businessServiceError: BusinessServiceError): Json =
    List(JsonError(businessServiceError.code, businessServiceError.description)).asJson

  def createUnrecognisedMessageTypeError: Json = List(JsonError(INVA002, "Invalid OMGMessageTypeID")).asJson

  def businessRequestValidationErrorToReplyMessage(
    errors: NonEmptyChain[BusinessRequestValidationError]
  ): Json = errors
    .map(error => toErrorMessage(error))
    .filter(_.isDefined)
    .map(_.get)
    .toList
    .asJson

  private def toErrorMessage(genericRequestValidationError: LocalMessageValidationError): Option[JsonError] =
    genericRequestValidationError match {
      case MissingJMSMessageID  => Some(JsonError(MISS001, "Missing JMSMessageID"))
      case InvalidJMSMessageID  => Some(JsonError(INVA001, "Invalid JMSMessageID"))
      case MissingMessageTypeID => Some(JsonError(MISS002, "Missing OMGMessageTypeID"))
      case InvalidMessageTypeID => Some(JsonError(INVA002, "Invalid OMGMessageTypeID"))
      case MissingApplicationID => Some(JsonError(MISS003, "Missing OMGApplicationID"))
      case InvalidApplicationID => Some(JsonError(INVA003, "Invalid OMGApplicationID"))
      case MissingJMSTimestamp  => Some(JsonError(MISS004, "Missing JMSTimestamp"))
      case MissingMessageFormat => Some(JsonError(MISS005, "Missing OMGMessageFormat"))
      case InvalidMessageFormat => Some(JsonError(INVA005, "Invalid OMGMessageFormat"))
      case MissingAuthToken     => Some(JsonError(MISS006, "Missing OMGToken"))
      case InvalidAuthToken     => Some(JsonError(INVA006, "Invalid OMGToken"))
      case MissingReplyAddress  => Some(JsonError(MISS007, "Missing OMGReplyAddress"))
      case InvalidReplyAddress  => Some(JsonError(INVA007, "Invalid OMGReplyAddress"))
    }

  private def toErrorMessage(businessRequestValidationError: BusinessRequestValidationError): Option[JsonError] =
    businessRequestValidationError match {
      case TextIsNonEmptyCharacters(message, _) => Some(JsonError(BLAN001, s"Message text is blank: $message"))
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
        m.setJMSCorrelationId(requestMessage.jmsMessageId)
        (m, outputQueue)
      }
    } *> IO.unit

  override def sendProcessingError(
    businessServiceError: BusinessServiceError,
    requestMessage: ValidatedLocalMessage
  ): IO[Unit] =
    jmsProducer.send { mf =>
      val msg = mf.makeTextMessage(createProcessingErrorMessage(businessServiceError).asJson.toString())
      msg.map { replyMessage =>
        replyMessage.setJMSCorrelationId(requestMessage.jmsMessageId)
        replyMessage.setStringProperty(
          MessageProperties.OMGApplicationID,
          "1234"
        ) // TODO(RW) this should be set as a constant
        replyMessage.setStringProperty(
          MessageProperties.OMGMessageTypeID,
          OutgoingMessageType.ProcessingError.entryName
        )
        replyMessage.setStringProperty(
          MessageProperties.OMGMessageFormat,
          "application/json"
        ) // TODO(RW) this should be set as a constant
        (replyMessage, outputQueue)
      }
    } *> IO.unit

  override def sendInvalidMessageFormatError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] =
    sendError(
      localMessage,
      OutgoingMessageType.InvalidMessageFormatError,
      localMessageValidationErrorsToReplyMessage(errors)
    )

  override def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    businessRequestValidationErrors: NonEmptyChain[BusinessRequestValidationError]
  ): IO[Unit] =
    sendError(
      localMessage,
      OutgoingMessageType.GeneralError,
      businessRequestValidationErrorToReplyMessage(businessRequestValidationErrors)
    )

  override def sendUnrecognisedMessageTypeError(localMessage: LocalMessage): IO[Unit] =
    sendError(
      localMessage,
      OutgoingMessageType.UnrecognisedMessageTypeError,
      createUnrecognisedMessageTypeError
    )

  override def sendInvalidApplicationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] =
    sendError(
      localMessage,
      OutgoingMessageType.InvalidApplicationError,
      localMessageValidationErrorsToReplyMessage(errors)
    )

  private def sendError(localMessage: LocalMessage, outgoingMessageType: OutgoingMessageType, jsonErrors: Json) =
    localMessage.jmsMessageId
      .filter(_.trim.nonEmpty)
      .map { requestMessageId =>
        jmsProducer.send { mf =>
          val msg = mf.makeTextMessage(
            jsonErrors.asJson.toString()
          )
          msg.map { replyMessage =>
            replyMessage.setJMSCorrelationId(requestMessageId)
            replyMessage.setStringProperty("OMGApplicationID", "1234") // TODO(RW) this should be set as a constant
            replyMessage.setStringProperty(
              "OMGMessageTypeID",
              outgoingMessageType.entryName
            ) // TODO(RW) this should be set as a constant
            replyMessage.setStringProperty(
              "OMGMessageFormat",
              "application/json"
            ) // TODO(RW) this should be set as a constant
            (replyMessage, outputQueue)
          }
        } *> IO.unit
      }
      .getOrElse(IO.pure {})
}
