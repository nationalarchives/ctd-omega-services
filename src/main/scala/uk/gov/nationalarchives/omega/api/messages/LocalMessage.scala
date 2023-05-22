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

package uk.gov.nationalarchives.omega.api.messages

import cats.data.ValidatedNec
import cats.effect.IO
import cats.syntax.all._
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import uk.gov.nationalarchives.omega.api.common.{ AppLogger, Version1UUID }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{ InvalidApplicationID, InvalidAuthToken, InvalidJMSMessageID, InvalidMessageFormat, InvalidMessageTypeID, InvalidReplyAddress, MissingApplicationID, MissingAuthToken, MissingJMSMessageID, MissingJMSTimestamp, MissingMessageFormat, MissingMessageTypeID, MissingReplyAddress, ValidationResult, acceptableMimeTypes, patternForApplicationId, patternForReplyAddress, patternForServiceId }

import java.time.{ Instant, LocalDateTime, ZoneOffset }
import scala.util.matching.Regex

/** @param persistentMessageId
  *   A unique identifier for the message within the ctd-omega-services application.
  * @param messageText
  *   The message payload, usually in JSON format.
  * @param omgMessageTypeId
  *   The Identifier of the Omega Message Type. In AsyncAPI Schema this is the messageId property of a message defined
  *   in the schema.
  * @param jmsMessageId
  *   A unique identifier for the message. We suggest using a short application instance specific prefix followed by a
  *   unique identifier. In the extreme case a UUID may be used. Depending on your JMS provider, this may be set on your
  *   behalf.
  * @param omgApplicationId
  *   Identifier of the application that is publishing the message to Omega.
  * @param jmsTimestamp
  *   The time the message was sent. The value of the timestamp is the amount of time, measured in milliseconds, that
  *   has elapsed since midnight, January 1, 1970, UTC. Depending on your JMS provider, this may be set on your behalf.
  * @param omgMessageFormat
  *   The Internet Media-Type indicating the format of the message body.
  * @param omgToken
  *   An Authentication Token that is valid for the OMGApplicationID and request.
  * @param omgReplyAddress
  *   The address that the reply to this message should be sent to. Typically this is the name of a Queue setup for a
  *   specific application and must be prefixed with the OMGApplicationID of the application that is making the request.
  */
@SerialVersionUID(1L)
final case class LocalMessage(
  persistentMessageId: Version1UUID,
  messageText: String,
  omgMessageTypeId: Option[String],
  jmsMessageId: Option[String],
  omgApplicationId: Option[String],
  jmsTimestamp: Option[Long],
  omgMessageFormat: Option[String],
  omgToken: Option[String],
  omgReplyAddress: Option[String]
) extends Serializable {

  def validateOmgApplicationId: ValidationResult[String] =
    omgApplicationId match {
      case Some(applicationId) if patternForApplicationId.matches(applicationId) => applicationId.validNec
      case Some(_)                                                               => InvalidApplicationID.invalidNec
      case None                                                                  => MissingApplicationID.invalidNec
    }

  def validateOmgToken: ValidationResult[String] =
    omgToken match {
      case Some(token) if token.trim.nonEmpty => token.validNec
      case Some(_)                            => InvalidAuthToken.invalidNec
      case None                               => MissingAuthToken.invalidNec
    }

  def validateOtherHeaders(
    validatedOmgToken: ValidationResult[String],
    validatedOmgApplicationId: ValidationResult[String]
  ): ValidationResult[ValidatedLocalMessage] =
    (
      validateJmsMessageId(jmsMessageId),
      validateOmgMessageTypeId,
      validatedOmgApplicationId,
      validateJmsTimestamp(jmsTimestamp),
      validateOmgMessageFormat(omgMessageFormat),
      validatedOmgToken,
      validateOmgReplyAddress(omgReplyAddress, omgApplicationId)
    )
      .mapN {
        (
          validatedJmsMessageId,
          validatedOmgMessageTypeId,
          validatedOmgApplicationId,
          validatedJmsTimestamp,
          validatedOmgMessageFormat,
          validatedOmgToken,
          validatedOmgReplyAddress
        ) =>
          ValidatedLocalMessage(
            persistentMessageId,
            validatedOmgMessageTypeId,
            messageText,
            validatedJmsMessageId,
            validatedOmgApplicationId,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(validatedJmsTimestamp), ZoneOffset.UTC),
            validatedOmgMessageFormat,
            validatedOmgToken,
            QueueName(validatedOmgReplyAddress)
          )

      }

  private def validateJmsMessageId(
    jmsMessageIdOpt: Option[String]
  ): ValidationResult[String] =
    jmsMessageIdOpt match {
      case Some(jmsMessageId) if jmsMessageId.trim.nonEmpty => jmsMessageId.validNec
      case Some(_)                                          => InvalidJMSMessageID.invalidNec
      case None                                             => MissingJMSMessageID.invalidNec
    }

  private def validateOmgMessageTypeId: ValidationResult[String] =
    omgMessageTypeId match {
      case Some(messageTypeId) if patternForServiceId.matches(messageTypeId) => messageTypeId.validNec
      case None                                                              => MissingMessageTypeID.invalidNec
      case _                                                                 => InvalidMessageTypeID.invalidNec
    }

  private def validateJmsTimestamp(
    jmsTimestampOpt: Option[Long]
  ): ValidationResult[Long] =
    jmsTimestampOpt match {
      case Some(jmsTimestamp) => jmsTimestamp.validNec
      case None               => MissingJMSTimestamp.invalidNec
    }

  private def validateOmgMessageFormat(
    omgMessageFormatOpt: Option[String]
  ): ValidationResult[String] = {

    def normalise(messageFormat: String): String = messageFormat.trim.toLowerCase

    omgMessageFormatOpt match {
      case Some(omgMessageFormat) if acceptableMimeTypes.contains(normalise(omgMessageFormat)) =>
        normalise(omgMessageFormat).validNec
      case Some(_) => InvalidMessageFormat.invalidNec
      case None    => MissingMessageFormat.invalidNec
    }
  }

  private def validateOmgReplyAddress(
    omgReplyAddressOpt: Option[String],
    omgApplicationIdOpt: Option[String]
  ): ValidationResult[String] =
    (omgReplyAddressOpt, omgApplicationIdOpt) match {
      case (Some(omgReplyddress), Some(omgApplicationId))
          if patternForReplyAddress.matches(omgReplyddress) && omgReplyddress.startsWith(omgApplicationId) =>
        omgReplyddress.validNec
      case (None, _) => MissingReplyAddress.invalidNec
      case _         => InvalidReplyAddress.invalidNec
    }
}
object LocalMessage extends AppLogger {

  type ValidationResult[A] = ValidatedNec[LocalMessageValidationError, A]

  val patternForApplicationId: Regex = "[A-Z]{4}([1-9][0-9][0-9]|0[1-9][0-9]|00[1-9])".r
  val patternForReplyAddress: Regex = "[A-Z]{4}([1-9][0-9][0-9]|0[1-9][0-9]|00[1-9])_([A-Za-z0-9_])+".r
  val patternForServiceId: Regex = "(OS|OD|OE)(LI|GE|UP|CR|RE)(S|F)[A-Z]{3}([1-9][0-9][0-9]|0[1-9][0-9]|00[1-9])".r

  val acceptableMimeTypes: Set[String] = Set("application/json")

  def createLocalMessage(
    persistentMessageId: Version1UUID,
    jmsMessage: JmsMessage
  ): IO[LocalMessage] =
    jmsMessage.asTextF[IO].attempt.map { result =>
      val messageText = result match {
        case Right(text) => text
        case Left(e) =>
          logger.error(s"Failed to retrieve message content due to ${e.getMessage}")
          ""
      }
      LocalMessage(
        persistentMessageId,
        messageText,
        jmsMessage.getStringProperty("OMGMessageTypeID"),
        jmsMessage.getJMSMessageId,
        jmsMessage.getStringProperty("OMGApplicationID"),
        jmsMessage.getJMSTimestamp,
        jmsMessage.getStringProperty("OMGMessageFormat"),
        jmsMessage.getStringProperty("OMGToken"),
        jmsMessage.getStringProperty("OMGReplyAddress")
      )
    }

  sealed trait LocalMessageValidationError

  object MissingJMSMessageID extends LocalMessageValidationError
  object InvalidJMSMessageID extends LocalMessageValidationError
  object MissingMessageTypeID extends LocalMessageValidationError
  object InvalidMessageTypeID extends LocalMessageValidationError
  object MissingApplicationID extends LocalMessageValidationError
  object InvalidApplicationID extends LocalMessageValidationError
  object MissingJMSTimestamp extends LocalMessageValidationError
  object MissingMessageFormat extends LocalMessageValidationError
  object InvalidMessageFormat extends LocalMessageValidationError
  object MissingAuthToken extends LocalMessageValidationError
  object InvalidAuthToken extends LocalMessageValidationError
  object MissingReplyAddress extends LocalMessageValidationError
  object InvalidReplyAddress extends LocalMessageValidationError

}
