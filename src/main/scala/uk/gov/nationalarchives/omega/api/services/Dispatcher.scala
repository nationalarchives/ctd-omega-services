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

import cats.data.Validated.{ Invalid, Valid }
import cats.data.{ Validated, ValidatedNec }
import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.business.echo.{ EchoRequest, EchoService }
import uk.gov.nationalarchives.omega.api.business.legalstatus.{ LegalStatusRequest, LegalStatusService }
import uk.gov.nationalarchives.omega.api.common.AppLogger
import uk.gov.nationalarchives.omega.api.messages.IncomingMessageType.{ ECHO001, OSLISALS001 }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.ValidationResult
import uk.gov.nationalarchives.omega.api.messages.{ IncomingMessageType, LocalMessage, LocalMessageStore, ValidatedLocalMessage }

import scala.util.Try

class Dispatcher(
  val localProducer: LocalProducer,
  localMessageStore: LocalMessageStore,
  echoService: EchoService,
  legalStatusService: LegalStatusService
) extends AppLogger {

  import cats.syntax.all._

  def runRecovery(dispatcherId: Int)(recoveredMessages: List[LocalMessage]): IO[Unit] =
    IO {
      recoveredMessages.foreach { recoveredMessage =>
        processMessage(recoveredMessage, dispatcherId).unsafeRunSync()
      }
    }

  def run(dispatcherId: Int)(q: Queue[IO, LocalMessage]): IO[Unit] =
    for {
      requestMessage <- q.take
      _              <- processMessage(requestMessage, dispatcherId)
    } yield ()

  private def processMessage(localMessage: LocalMessage, dispatcherId: Int): IO[Unit] =
    for {
      _ <- logger.info(s"processing message: ${localMessage.messageText}")
      _ <- logger.info(
             s"Dispatcher # $dispatcherId, processing message id: ${localMessage.persistentMessageId}"
           )
      _ <- checkAndReply(localMessage)
      _ <- remove(localMessage)
    } yield ()

  private def checkAndReply(localMessage: LocalMessage): IO[Unit] =
    localMessage.validateOmgApplicationId match {
      case Valid(applicationId) => checkAuthToken(applicationId.validNec, localMessage)
      case Invalid(errors)      => localProducer.sendInvalidApplicationError(localMessage, errors)
    }

  private def checkAuthToken(applicationId: ValidationResult[String], localMessage: LocalMessage): IO[Unit] =
    localMessage.validateOmgToken match {
      case Valid(omgToken) => checkOtherHeaders(omgToken.validNec, applicationId, localMessage)
      case Invalid(errors) => localProducer.sendAuthenticationError(localMessage, errors)
    }

  private def checkOtherHeaders(
    omgToken: ValidationResult[String],
    applicationId: ValidationResult[String],
    localMessage: LocalMessage
  ): IO[Unit] =
    localMessage.validateOtherHeaders(omgToken, applicationId) match {
      case Valid(validatedLocalMessage) =>
        createAndValidateServiceRequest(validatedLocalMessage, localMessage)
      case Invalid(errors) =>
        localProducer.sendInvalidMessageFormatError(localMessage, errors)
    }

  private def createAndValidateServiceRequest(
    validatedLocalMessage: ValidatedLocalMessage,
    originalLocalMessage: LocalMessage
  ): IO[Unit] =
    IncomingMessageType.withNameOption(validatedLocalMessage.omgMessageTypeId) match {
      case Some(messageType) =>
        val (businessService: BusinessService, businessServiceRequest: BusinessServiceRequest) =
          createServiceRequest(validatedLocalMessage, messageType)
        validateBusinessServiceRequest(businessService, businessServiceRequest) match {
          case Validated.Valid(validatedBusinessServiceRequest) =>
            sendResultToJmsQueue(
              execBusinessService(businessService, validatedBusinessServiceRequest),
              validatedLocalMessage
            )
          case Validated.Invalid(errors) => localProducer.sendWhenBusinessRequestIsInvalid(originalLocalMessage, errors)
        }
      case None => localProducer.sendUnrecognisedMessageTypeError(originalLocalMessage)
    }

  private def createServiceRequest(
    localMessage: ValidatedLocalMessage,
    messageType: IncomingMessageType
  ): (BusinessService, BusinessServiceRequest) =
    messageType match {
      case ECHO001     => (echoService, EchoRequest(Some(localMessage.messageText)))
      case OSLISALS001 => (legalStatusService, LegalStatusRequest())
      // add more service IDs here
    }

  // TODO: This is where we do the second and final stage of validation.
  private def validateBusinessServiceRequest(
    businessService: BusinessService,
    businessServiceRequest: BusinessServiceRequest
  ): ValidatedNec[BusinessRequestValidationError, BusinessServiceRequest] =
    businessService match {
      case value: BusinessRequestValidation =>
        value.validateRequest(businessServiceRequest)
      case _ =>
        Validated.valid(businessServiceRequest)
    }

  private def execBusinessService[T <: BusinessServiceRequest, U <: BusinessServiceReply, E <: BusinessServiceError](
    businessService: BusinessService,
    validatedBusinessServiceRequest: T
  ): Either[BusinessServiceError, BusinessServiceReply] =
    businessService.process(validatedBusinessServiceRequest)

  private def sendResultToJmsQueue[U <: BusinessServiceReply, E <: BusinessServiceError](
    businessResult: Either[E, U],
    requestMessage: ValidatedLocalMessage
  ): IO[Unit] =
    businessResult match {
      case Right(businessResult) =>
        localProducer.send(businessResult.content, requestMessage)
      case Left(serviceError) =>
        localProducer.sendProcessingError(serviceError, requestMessage)

    }

  private def remove(localMessage: LocalMessage): IO[Try[Unit]] =
    localMessageStore.removeMessage(localMessage.persistentMessageId)

}
