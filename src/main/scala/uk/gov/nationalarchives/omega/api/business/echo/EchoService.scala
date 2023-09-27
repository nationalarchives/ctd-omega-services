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

package uk.gov.nationalarchives.omega.api.business.echo

import cats.data.{ NonEmptyChain, Validated }
import cats.effect.IO
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{ InvalidMessagePayload, ValidationResult }
import uk.gov.nationalarchives.omega.api.messages.ValidatedLocalMessage
import uk.gov.nationalarchives.omega.api.messages.request.{ EchoRequest, RequestMessage }
class EchoService extends BusinessService with BusinessRequestValidation {

  override def validateRequest(validatedLocalMessage: ValidatedLocalMessage): ValidationResult[RequestMessage] =
    Validated.cond(
      validatedLocalMessage.messageText.nonEmpty,
      EchoRequest(Some(validatedLocalMessage.messageText)),
      NonEmptyChain.one(InvalidMessagePayload(Some("Echo Text cannot be empty.")))
    )

  override def process(requestMessage: RequestMessage): IO[Either[BusinessServiceError, BusinessServiceReply]] =
    requestMessage match {
      case EchoRequest(text) =>
        if (text.nonEmpty && text.get.contains("ERROR")) {
          IO(Left(EchoExplicitError(s"Explicit error: ${text.get}")))
        } else {
          IO(Right(EchoReply(s"The Echo Service says: ${text.getOrElse("")}")))
        }
      case _ => IO(Left(EchoExplicitError(s"Unexpected message type")))
    }

}
