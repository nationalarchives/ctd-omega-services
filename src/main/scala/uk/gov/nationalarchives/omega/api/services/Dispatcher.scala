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

import cats.data.{ Validated, ValidatedNec }
import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.business.echo.{ EchoRequest, EchoService }
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.services.ServiceIdentifier.ECHO001

import scala.util.Try

class Dispatcher(val localProducer: LocalProducer, localMessageStore: LocalMessageStore, echoService: EchoService) {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  import cats.syntax.all._

  def runRecovery(dispatcherId: Int)(recoveredMessages: List[LocalMessage]): IO[Unit] = {
    println("Starting recovery...")
    IO {
      recoveredMessages.foreach { recoveredMessage =>
        processMessage(recoveredMessage, dispatcherId).unsafeRunSync()
      }
    }
  }

  def run(dispatcherId: Int)(q: Queue[IO, LocalMessage]): IO[Unit] =
    for {
      requestMessage <- q.take
      res            <- processMessage(requestMessage, dispatcherId)
    } yield ()

  private def processMessage(localMessage: LocalMessage, dispatcherId: Int): IO[Unit] = {
    println(s"Processing message ${localMessage.persistentMessageId}")
    for {
      _ <- logger.info(s"processing message: ${localMessage.messageText}")
      _ <- logger.info(s"Dispatcher # $dispatcherId, processing message id: ${localMessage.persistentMessageId}")
      _ <- process(localMessage)
      _ <- remove(localMessage)
    } yield ()
  }

  private def process(localMessage: LocalMessage): IO[Unit] =
    localMessage.validate() match {
      case Validated.Valid(validatedLocalMessage: ValidatedLocalMessage) =>
        createAndValidateServiceRequest(validatedLocalMessage, localMessage)
      case Validated.Invalid(errors) => localProducer.sendWhenGenericRequestIsInvalid(localMessage, errors)
    }

  private def createAndValidateServiceRequest(
    validatedLocalMessage: ValidatedLocalMessage,
    originalLocalMessage: LocalMessage
  ): IO[Unit] = {

    val (businessService: BusinessService, businessServiceRequest: BusinessServiceRequest) =
      createServiceRequest(validatedLocalMessage)
    validateBusinessServiceRequest(businessService, businessServiceRequest) match {
      case Validated.Valid(validatedBusinessServiceRequest) =>
        sendResultToJmsQueue(
          execBusinessService(businessService, validatedBusinessServiceRequest),
          validatedLocalMessage
        )
      case Validated.Invalid(errors) => localProducer.sendWhenBusinessRequestIsInvalid(originalLocalMessage, errors)
    }
  }

  private def createServiceRequest(localMessage: ValidatedLocalMessage): (BusinessService, BusinessServiceRequest) =
    localMessage.serviceId match {
      case ECHO001 =>
        (echoService, EchoRequest(localMessage.messageText))
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

  private def execBusinessService[T <: BusinessServiceRequest, U <: BusinessServiceResponse, E <: BusinessServiceError](
    businessService: BusinessService,
    validatedBusinessServiceRequest: T
  ): Either[BusinessServiceError, BusinessServiceResponse] =
    businessService.process(validatedBusinessServiceRequest)

  private def sendResultToJmsQueue[U <: BusinessServiceResponse, E <: BusinessServiceError](
    businessResult: Either[E, U],
    requestMessage: ValidatedLocalMessage
  ): IO[Unit] = {
    val replyMessage: String =
      businessResult match {
        case Right(businessResult) =>
          requestMessage.serviceId match {
            case ECHO001 => businessResult.content
            // TODO(RW) add more services here
          }
        case Left(serviceError) =>
          s"""{status: "SERVICE-ERROR", reference: "$getCustomerErrorReference", code: "${serviceError.code}", message: "${serviceError.message}"}"""

      }
    localProducer.send(replyMessage, requestMessage)
  } // TODO(RW) add recoverWith here to handle unexpected exceptions and send a message back to the client

  private def getCustomerErrorReference: Version1UUID = Version1UUID.generate()

  private def remove(localMessage: LocalMessage): IO[Try[Unit]] =
    localMessageStore.removeMessage(localMessage.persistentMessageId)

}
