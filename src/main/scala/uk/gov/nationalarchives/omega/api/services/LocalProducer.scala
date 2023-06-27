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
import uk.gov.nationalarchives.omega.api.ApiServiceApp
import uk.gov.nationalarchives.omega.api.business.BusinessServiceError
import uk.gov.nationalarchives.omega.api.common.ErrorCode._
import uk.gov.nationalarchives.omega.api.common.{ AppLogger, JsonError }
import uk.gov.nationalarchives.omega.api.messages.IncomingMessageType.OSLISALS001
import uk.gov.nationalarchives.omega.api.messages.LocalMessage._
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, MessageProperties, OutgoingMessageType, ValidatedLocalMessage }

trait LocalProducer {

  val jsonContentType = "application/json"
  def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit]
  def sendProcessingError(businessServiceError: BusinessServiceError, requestMessage: ValidatedLocalMessage): IO[Unit]

  def sendInvalidMessageFormatError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.MessageValidationError]
  ): IO[Unit]

  def sendUnrecognisedMessageTypeError(localMessage: LocalMessage): IO[Unit]

  def sendInvalidApplicationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.MessageValidationError]
  ): IO[Unit]

  def sendAuthenticationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.MessageValidationError]
  ): IO[Unit]

  def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    errors: NonEmptyChain[MessageValidationError]
  ): IO[Unit]

  def localMessageValidationErrorsToReplyMessage(
    localMessageValidationErrors: NonEmptyChain[LocalMessage.MessageValidationError]
  ): Json = localMessageValidationErrors
    .map(localMessageValidationError => toErrorMessage(localMessageValidationError))
    .filter(_.isDefined)
    .map(_.get)
    .toList
    .asJson

  def createProcessingErrorMessage(businessServiceError: BusinessServiceError): Json =
    List(JsonError(businessServiceError.code, businessServiceError.description)).asJson

  def createUnrecognisedMessageTypeError: Json = List(JsonError(INVA002, "Invalid OMGMessageTypeID")).asJson

  def messageValidationErrorToReplyMessage(
    errors: NonEmptyChain[MessageValidationError]
  ): Json = errors
    .map(error => toErrorMessage(error))
    .filter(_.isDefined)
    .map(_.get)
    .toList
    .asJson

  private def toErrorMessage(genericRequestValidationError: MessageValidationError): Option[JsonError] =
    genericRequestValidationError match {
      case MissingJMSMessageID(_, _)               => Some(JsonError(MISS001, "Missing JMSMessageID"))
      case InvalidJMSMessageID(_, _)               => Some(JsonError(INVA001, "Invalid JMSMessageID"))
      case MissingMessageTypeID(_, _)              => Some(JsonError(MISS002, "Missing OMGMessageTypeID"))
      case InvalidMessageTypeID(_, _)              => Some(JsonError(INVA002, "Invalid OMGMessageTypeID"))
      case MissingApplicationID(_, _)              => Some(JsonError(MISS003, "Missing OMGApplicationID"))
      case InvalidApplicationID(_, _)              => Some(JsonError(INVA003, "Invalid OMGApplicationID"))
      case MissingJMSTimestamp(_, _)               => Some(JsonError(MISS004, "Missing JMSTimestamp"))
      case MissingMessageFormat(_, _)              => Some(JsonError(MISS005, "Missing OMGMessageFormat"))
      case InvalidMessageFormat(_, _)              => Some(JsonError(INVA005, "Invalid OMGMessageFormat"))
      case MissingAuthToken(_, _)                  => Some(JsonError(MISS006, "Missing OMGToken"))
      case InvalidAuthToken(_, _)                  => Some(JsonError(INVA006, "Invalid OMGToken"))
      case MissingReplyAddress(_, _)               => Some(JsonError(MISS007, "Missing OMGReplyAddress"))
      case InvalidReplyAddress(_, _)               => Some(JsonError(INVA007, "Invalid OMGReplyAddress"))
      case InvalidMessagePayload(Some(message), _) => Some(JsonError(INVA008, message))
      case InvalidMessagePayload(_, Some(cause)) =>
        Some(JsonError(INVA008, s"Message payload invalid due to ${cause.getMessage}"))
      case InvalidMessagePayload(_, _) => Some(JsonError(INVA008, "Message payload invalid"))
    }

}

/** In JMS terms a producer can have one or many destinations - in this implementation we have one destination, if we
  * want many destinations we need to modify the send method to pass the destination with each call
  */
class LocalProducerImpl(val jmsProducer: JmsProducer[IO]) extends LocalProducer with AppLogger {

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
        m.setStringProperty(MessageProperties.OMGApplicationID, ApiServiceApp.applicationId)
        m.setStringProperty(
          MessageProperties.OMGMessageFormat,
          jsonContentType
        )
        requestMessage.omgMessageTypeId match {
          case OSLISALS001.entryName =>
            m.setStringProperty(
              MessageProperties.OMGMessageTypeID,
              OutgoingMessageType.AssetLegalStatusSummaryList.entryName
            )
          case _ => ()

        }
        (m, requestMessage.omgReplyAddress)
      }
    } *> IO.unit

  override def sendProcessingError(
    businessServiceError: BusinessServiceError,
    requestMessage: ValidatedLocalMessage
  ): IO[Unit] =
    sendError(
      requestMessage.jmsMessageId,
      requestMessage.omgReplyAddress,
      OutgoingMessageType.ProcessingError,
      createProcessingErrorMessage(businessServiceError).asJson
    )

  override def sendInvalidMessageFormatError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.MessageValidationError]
  ): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.InvalidMessageFormatError,
      localMessageValidationErrorsToReplyMessage(errors)
    )

  override def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    businessRequestValidationErrors: NonEmptyChain[MessageValidationError]
  ): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.InvalidMessageError,
      messageValidationErrorToReplyMessage(businessRequestValidationErrors)
    )

  override def sendUnrecognisedMessageTypeError(localMessage: LocalMessage): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.UnrecognisedMessageTypeError,
      createUnrecognisedMessageTypeError
    )

  override def sendInvalidApplicationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.MessageValidationError]
  ): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.InvalidApplicationError,
      localMessageValidationErrorsToReplyMessage(errors)
    )

  override def sendAuthenticationError(
    localMessage: LocalMessage,
    error: NonEmptyChain[MessageValidationError]
  ): IO[Unit] =
    error.head match {
      case InvalidAuthToken(_, _) =>
        extractMessageIdAndSendError(
          localMessage,
          OutgoingMessageType.AuthenticationError,
          localMessageValidationErrorsToReplyMessage(error)
        )
      case _ =>
        extractMessageIdAndSendError(
          localMessage,
          OutgoingMessageType.InvalidMessageFormatError,
          localMessageValidationErrorsToReplyMessage(error)
        )
    }

  private def extractMessageIdAndSendError(
    localMessage: LocalMessage,
    outgoingMessageType: OutgoingMessageType,
    jsonErrors: Json
  ): IO[Unit] = {
    val result: Option[(String, String)] = for {
      messageId    <- localMessage.jmsMessageId
      replyAddress <- localMessage.omgReplyAddress
    } yield (messageId, replyAddress)
    result match {
      case Some((messageId, replyAddress)) =>
        sendError(messageId, QueueName(replyAddress), outgoingMessageType, jsonErrors)
      case _ =>
        logger.error(
          s"Unable to reply to message ${localMessage.persistentMessageId} due to missing messageId or replyAddress."
        )
    }
  }

  private def sendError(
    requestMessageId: String,
    replyQueue: QueueName,
    outgoingMessageType: OutgoingMessageType,
    jsonErrors: Json
  ): IO[Unit] =
    jmsProducer.send { mf =>
      val msg = mf.makeTextMessage(
        jsonErrors.asJson.toString()
      )
      msg.map { replyMessage =>
        replyMessage.setJMSCorrelationId(requestMessageId)
        replyMessage.setStringProperty(MessageProperties.OMGApplicationID, ApiServiceApp.applicationId)
        replyMessage.setStringProperty(
          MessageProperties.OMGMessageTypeID,
          outgoingMessageType.entryName
        )
        replyMessage.setStringProperty(
          MessageProperties.OMGMessageFormat,
          jsonContentType
        )
        (replyMessage, replyQueue)
      }
    } *> IO.unit

}
