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
import cats.implicits.catsSyntaxEitherId
import jms4s.jms.JmsMessage

import java.util.UUID

case class LocalMessage(
  persistentMessageId: UUID,
  serviceId: ServiceIdentifier,
  messageText: String,
  correlationId: String
)
object LocalMessage {

  private def getServiceId(jmsMessage: JmsMessage): Either[ServiceError, ServiceIdentifier] =
    jmsMessage.getStringProperty("sid") match {
      case Some(sid) =>
        ServiceIdentifier.withNameOption(sid.toUpperCase) match {
          case Some(serviceId) => Right(serviceId)
          case None            => Left(ServiceIdentifierError("SID not recognised"))
        }
      case None => Left(ServiceIdentifierError("Missing service ID"))
    }

  private def getMessageId(jmsMessage: JmsMessage): Either[ServiceError, String] =
    jmsMessage.getJMSMessageId match {
      case Some(messageId) => Right(messageId)
      case None            => Left(MessageIdentifierError("Missing message ID"))
    }

  def createLocalMessage(
    persistentMessageId: UUID,
    jmsMessage: JmsMessage
  ): IO[Either[ServiceError, LocalMessage]] =
    jmsMessage.asTextF[IO].attempt.map {
      case Left(e) => MessageReadError("Unable to read message", Some(e)).asLeft[LocalMessage]
      case Right(text) =>
        for {
          serviceId     <- getServiceId(jmsMessage)
          correlationId <- getMessageId(jmsMessage)
        } yield LocalMessage(persistentMessageId, serviceId, text, correlationId)
    }

}
