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
import jms4s.jms.JmsMessage
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.common.Version1UUID

final case class LocalMessage(
  persistentMessageId: Version1UUID,
  messageText: String,
  serviceId: Option[ServiceIdentifier],
  correlationId: Option[String]
) {
  def validate: ValidatedLocalMessage =
    // TODO(RW) this method will need to validated instance of the message - currently we are just adding some default values
    ValidatedLocalMessage(
      persistentMessageId,
      serviceId.getOrElse(ServiceIdentifier.ECHO001),
      messageText,
      correlationId.getOrElse("1234")
    )
}
object LocalMessage {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  private def getServiceId(jmsMessage: JmsMessage): Option[ServiceIdentifier] =
    for {
      sid       <- jmsMessage.getStringProperty("sid")
      serviceId <- ServiceIdentifier.withNameOption(sid.toUpperCase)
    } yield serviceId

  def createLocalMessage(
    persistentMessageId: Version1UUID,
    jmsMessage: JmsMessage
  ): IO[LocalMessage] =
    jmsMessage.asTextF[IO].attempt.map {
      case Right(text) => LocalMessage(persistentMessageId, text, getServiceId(jmsMessage), jmsMessage.getJMSMessageId)
      case Left(e) =>
        logger.error(s"Failed to retrieve message content due to ${e.getMessage}")
        LocalMessage(persistentMessageId, "", getServiceId(jmsMessage), jmsMessage.getJMSMessageId)
    }

}
