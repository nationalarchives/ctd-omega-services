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
import cats.implicits.{catsSyntaxTuple4Semigroupal, catsSyntaxValidatedIdBinCompat0}
import jms4s.jms.JmsMessage
import uk.gov.nationalarchives.omega.api.common.{ErrorCode, ValidationError}
import uk.gov.nationalarchives.omega.api.services.ServiceIdentifier

import java.util.UUID

final case class LocalMessage(
  persistentMessageId: UUID,
  messageText: String,
  serviceId: Option[ServiceIdentifier],
  correlationId: Option[String]
) {

  def validate: MessageValidationResult[ValidatedLocalMessage] = {
    (persistentMessageId.validNec, validateServiceId, validateMessageText, validateMessageId).mapN(ValidatedLocalMessage)
  }

  type MessageValidationResult[A] = ValidatedNec[ValidationError,A]

  private def validateMessageId: MessageValidationResult[String] =
    if(correlationId.nonEmpty) {
      correlationId.get.validNec
    } else {
      ValidationError(ErrorCode.MessageIdentifierError, "Message ID not found", correlationId).invalidNec
    }

  private def validateServiceId: MessageValidationResult[ServiceIdentifier] =
    if(serviceId.nonEmpty) {
      serviceId.get.validNec
    } else {
      ValidationError(ErrorCode.ServiceIdentifierError, "SID not found or not recognised", correlationId).invalidNec
    }

  private def validateMessageText: MessageValidationResult[String] =
    if(messageText.trim.nonEmpty) {
      messageText.validNec
    } else {
      ValidationError(ErrorCode.EmptyMessageError, "Message is empty", correlationId).invalidNec
    }

}
object LocalMessage {

  type MessageValidationResult[A] = ValidatedNec[ValidationError,A]

  def createLocalMessage(
    persistentMessageId: UUID,
    jmsMessage: JmsMessage
  ): IO[LocalMessage] =
    jmsMessage.asTextF[IO].attempt.map {
      case Right(text) => LocalMessage(persistentMessageId, text, getServiceId(jmsMessage), jmsMessage.getJMSMessageId)
      case Left(e) =>
        // TODO(RW) log the error
        LocalMessage(persistentMessageId,"", getServiceId(jmsMessage), jmsMessage.getJMSMessageId)
    }

  private def getServiceId(jmsMessage: JmsMessage): Option[ServiceIdentifier] =
    for {
      sid <- jmsMessage.getStringProperty("sid")
      serviceId <- ServiceIdentifier.withNameOption(sid.toUpperCase)
    } yield serviceId

}
