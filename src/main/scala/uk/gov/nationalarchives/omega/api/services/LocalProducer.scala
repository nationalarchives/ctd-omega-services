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
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import uk.gov.nationalarchives.omega.api.ApiServiceApp
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidationError, BusinessServiceError, TextIsNonEmptyCharacters }
import uk.gov.nationalarchives.omega.api.common.ErrorCode._
import uk.gov.nationalarchives.omega.api.common.JsonError
import uk.gov.nationalarchives.omega.api.messages.IncomingMessageType.OSLISALS001
import uk.gov.nationalarchives.omega.api.messages.LocalMessage._
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, MessageProperties, OutgoingMessageType, ValidatedLocalMessage }

trait LocalProducer {

  val jsonContentType = "application/json"
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

  def sendAuthenticationError(
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
class LocalProducerImpl(val jmsProducer: JmsProducer[IO]) extends LocalProducer {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  /** Send the given reply message to the output queue
    * @param replyMessage
    *   \- the message
    * @param requestMessage
    *   the request message (needed for correlation ID)
    * @return
    */
  def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit] =
    IO.println(s"Reply address: ${requestMessage.omgReplyAddress.value}") *>
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
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.InvalidMessageFormatError,
      localMessageValidationErrorsToReplyMessage(errors)
    )

  override def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    businessRequestValidationErrors: NonEmptyChain[BusinessRequestValidationError]
  ): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.InvalidMessageError,
      businessRequestValidationErrorToReplyMessage(businessRequestValidationErrors)
    )

  override def sendUnrecognisedMessageTypeError(localMessage: LocalMessage): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.UnrecognisedMessageTypeError,
      createUnrecognisedMessageTypeError
    )

  override def sendInvalidApplicationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] =
    extractMessageIdAndSendError(
      localMessage,
      OutgoingMessageType.InvalidApplicationError,
      localMessageValidationErrorsToReplyMessage(errors)
    )

  override def sendAuthenticationError(
    localMessage: LocalMessage,
    error: NonEmptyChain[LocalMessageValidationError]
  ): IO[Unit] =
    error.head match {
      case InvalidAuthToken =>
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
        logger.error(s"Unable to reply to message ${localMessage.persistentMessageId} due to insufficient information.")
    }
  }

  private def sendError(
    requestMessageId: String,
    replyAddress: QueueName,
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
        (replyMessage, replyAddress)
      }
    } *> IO.unit

}
