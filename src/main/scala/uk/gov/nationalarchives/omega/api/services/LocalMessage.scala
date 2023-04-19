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

import cats.data.ValidatedNec
import cats.effect.IO
import cats.syntax.all._
import jms4s.jms.JmsMessage
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.services.LocalMessage.{ InvalidApplicationID, InvalidAuthToken, InvalidJMSMessageID, InvalidMessageFormat, InvalidResponseAddress, InvalidServiceID, MissingApplicationID, MissingAuthToken, MissingJMSMessageID, MissingJMSTimestamp, MissingMessageFormat, MissingResponseAddress, MissingServiceID, ValidationResult, acceptableMimeTypes, patternForApplicationId, patternForResponseAddress, patternForServiceId }

import java.time.{ Instant, LocalDateTime, ZoneOffset }
import scala.util.matching.Regex

@SerialVersionUID(1L)
final case class LocalMessage(
  persistentMessageId: Version1UUID,
  messageText: String,
  serviceId: Option[String],
  correlationId: Option[String],
  applicationId: Option[String],
  epochTimeInMilliseconds: Option[Long],
  messageFormat: Option[String],
  authToken: Option[String],
  responseAddress: Option[String]
) extends Serializable {

  def validate(): ValidationResult[ValidatedLocalMessage] =
    (
      validateCorrelationId(correlationId),
      validateServiceId(serviceId),
      validateApplicationId(applicationId),
      validateEpochTimeInMilliseconds(epochTimeInMilliseconds),
      validateMessageFormat(messageFormat),
      validateAuthToken(authToken),
      validateResponseAddress(responseAddress, applicationId)
    )
      .mapN {
        (
          validatedCorrelationId,
          validatedServiceId,
          validatedApplicationId,
          validatedEpochTimeInMilliseconds,
          validatedMessageFormat,
          validatedAuthToken,
          validatedResponseAddress
        ) =>
          ValidatedLocalMessage(
            persistentMessageId,
            validatedServiceId,
            messageText,
            validatedCorrelationId,
            validatedApplicationId,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(validatedEpochTimeInMilliseconds), ZoneOffset.UTC),
            validatedMessageFormat,
            validatedAuthToken,
            validatedResponseAddress
          )

      }

  private def validateCorrelationId(
    correlationIdOpt: Option[String]
  ): ValidationResult[String] =
    correlationIdOpt match {
      case Some(correlationId) if correlationId.trim.nonEmpty => correlationId.validNec
      case Some(_)                                            => InvalidJMSMessageID.invalidNec
      case None                                               => MissingJMSMessageID.invalidNec
    }

  private def validateServiceId(
    serviceIdOpt: Option[String]
  ): ValidationResult[ServiceIdentifier] =
    serviceIdOpt match {
      case Some(serviceId) if patternForServiceId.matches(serviceId) =>
        ServiceIdentifier
          .withNameOption(serviceId)
          .map(_.validNec)
          .getOrElse(InvalidServiceID.invalidNec)
      case None => MissingServiceID.invalidNec
      case _    => InvalidServiceID.invalidNec
    }

  private def validateApplicationId(
    applicationIdOpt: Option[String]
  ): ValidationResult[String] =
    applicationIdOpt match {
      case Some(applicationId) if patternForApplicationId.matches(applicationId) => applicationId.validNec
      case Some(_)                                                               => InvalidApplicationID.invalidNec
      case None                                                                  => MissingApplicationID.invalidNec
    }

  private def validateEpochTimeInMilliseconds(
    epochTimeInMillisecondsOpt: Option[Long]
  ): ValidationResult[Long] =
    epochTimeInMillisecondsOpt match {
      case Some(epochTimeInMilliseconds) => epochTimeInMilliseconds.validNec
      case None                          => MissingJMSTimestamp.invalidNec
    }

  private def validateMessageFormat(
    messageFormatOpt: Option[String]
  ): ValidationResult[String] = {

    def normalise(messageFormat: String): String = messageFormat.trim.toLowerCase

    messageFormatOpt match {
      case Some(messageFormat) if acceptableMimeTypes.contains(normalise(messageFormat)) =>
        normalise(messageFormat).validNec
      case Some(_) => InvalidMessageFormat.invalidNec
      case None    => MissingMessageFormat.invalidNec
    }
  }

  private def validateAuthToken(
    valueOpt: Option[String]
  ): ValidationResult[String] =
    valueOpt match {
      case Some(value) if value.trim.nonEmpty => value.validNec
      case Some(_)                            => InvalidAuthToken.invalidNec
      case None                               => MissingAuthToken.invalidNec
    }

  private def validateResponseAddress(
    responseAddressOpt: Option[String],
    applicationIdOpt: Option[String]
  ): ValidationResult[String] =
    (responseAddressOpt, applicationIdOpt) match {
      case (Some(responseAddress), Some(applicationId))
          if patternForResponseAddress.matches(responseAddress) && responseAddress.startsWith(applicationId) =>
        responseAddress.validNec
      case (None, _) => MissingResponseAddress.invalidNec
      case _         => InvalidResponseAddress.invalidNec
    }
}
object LocalMessage {

  type ValidationResult[A] = ValidatedNec[LocalMessageValidationError, A]

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  val patternForApplicationId: Regex = "[A-Z]{4}([1-9][0-9][0-9]|0[1-9][0-9]|00[1-9])".r
  val patternForResponseAddress: Regex = "[A-Z]{4}([1-9][0-9][0-9]|0[1-9][0-9]|00[1-9])(\\.[A-Za-z0-9])+".r
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
        jmsMessage.getStringProperty("OMGResponseAddress")
      )
    }

  sealed trait LocalMessageValidationError

  object MissingJMSMessageID extends LocalMessageValidationError
  object InvalidJMSMessageID extends LocalMessageValidationError
  object MissingServiceID extends LocalMessageValidationError
  object InvalidServiceID extends LocalMessageValidationError
  object MissingApplicationID extends LocalMessageValidationError
  object InvalidApplicationID extends LocalMessageValidationError
  object MissingJMSTimestamp extends LocalMessageValidationError
  object MissingMessageFormat extends LocalMessageValidationError
  object InvalidMessageFormat extends LocalMessageValidationError
  object MissingAuthToken extends LocalMessageValidationError
  object InvalidAuthToken extends LocalMessageValidationError
  object MissingResponseAddress extends LocalMessageValidationError
  object InvalidResponseAddress extends LocalMessageValidationError

}
