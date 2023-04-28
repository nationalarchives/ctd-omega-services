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
import jms4s.config.QueueName
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidationError, BusinessServiceError }
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, ValidatedLocalMessage }

class TestProducerImpl(val queueName: QueueName) extends LocalProducer {

  var message = ""

  override def send(replyMessage: String, requestMessage: ValidatedLocalMessage): IO[Unit] = {
    message = replyMessage
    IO.unit
  }

  override def sendInvalidMessageFormatError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] = {
    message = localMessageValidationErrorsToReplyMessage(errors).toString()
    IO.unit
  }

  override def sendWhenBusinessRequestIsInvalid(
    localMessage: LocalMessage,
    errors: NonEmptyChain[BusinessRequestValidationError]
  ): IO[Unit] = {
    message = businessRequestValidationErrorToReplyMessage(errors).toString()
    IO.unit
  }

  override def sendProcessingError(
    businessServiceError: BusinessServiceError,
    requestMessage: ValidatedLocalMessage
  ): IO[Unit] = IO.unit

  override def sendUnrecognisedMessageTypeError(localMessage: LocalMessage): IO[Unit] = IO.unit

  override def sendInvalidApplicationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] = {
    message = localMessageValidationErrorsToReplyMessage(errors).toString()
    IO.unit
  }

  override def sendAuthenticationError(
    localMessage: LocalMessage,
    errors: NonEmptyChain[LocalMessage.LocalMessageValidationError]
  ): IO[Unit] = {
    message = localMessageValidationErrorsToReplyMessage(errors).toString()
    IO.unit
  }
}
