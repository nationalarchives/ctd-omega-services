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

import cats.data.Validated.{Invalid, Valid}
import cats.data.{Validated, ValidatedNec}
import cats.effect.IO
import cats.effect.std.Queue
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import com.fasterxml.uuid.{EthernetAddress, Generators}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import sttp.tapir.ValidationResult
import uk.gov.nationalarchives.omega.api.business.RequestValidation.RequestValidationResult
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.business.echo.{EchoRequest, EchoService}
import uk.gov.nationalarchives.omega.api.common.{ErrorHandling, ValidationError}
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.MessageValidationResult
import uk.gov.nationalarchives.omega.api.messages.{LocalMessage, ValidatedLocalMessage}
import uk.gov.nationalarchives.omega.api.services.ServiceIdentifier.ECHO001

import java.util.UUID

class Dispatcher(override val localProducer: LocalProducer, echoService: EchoService) extends ErrorHandling {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  private val generator = Generators.timeBasedGenerator(EthernetAddress.fromInterface)

  def run(dispatcherId: Int)(queue: Queue[IO, LocalMessage]): IO[Unit] =
    for {
      requestMessage <- queue.take
      validatedRequestMessage <- IO.pure(validateLocalMessage(requestMessage))
//      validationResult <- retrieveAndValidateLocalMessage(queue)
//      requestMessage <- validationResult match {
//        case Right(validatedMessage) => IO.pure(validatedMessage)
//        case Left(error) => sendErrorMessage(error)
//      }
      _ <- logger.info(s"Dispatcher # $dispatcherId, processing message id: ${requestMessage.persistentMessageId}")
      //(businessService, businessServiceRequest) <- IO.pure(createServiceRequest(validatedRequestMessage))
      serviceRequest <- IO.pure(createServiceRequest(validatedRequestMessage))
      validatedBusinessServiceRequest <-
        IO.pure(validateBusinessServiceRequest(businessService, businessServiceRequest))
      businessResult <- execBusinessService(businessService, validatedBusinessServiceRequest)
      res            <- sendResultToJmsQueue(businessResult, validatedRequestMessage)
    } yield res

  private def sendErrorMessage(serviceError: ServiceError): IO[Unit] = IO.unit

  private def retrieveAndValidateLocalMessage(queue: Queue[IO, LocalMessage]): IO[MessageValidationResult[ValidatedLocalMessage]]= {
    queue.take.map( message => message.validate)
  }

  private def createServiceRequest(localMessage: MessageValidationResult[ValidatedLocalMessage]): MessageValidationResult[(BusinessService, BusinessServiceRequest)] =
    localMessage.map { message =>
      message.serviceId match {
        case ECHO001 =>
          Tuple2(
            echoService,
            EchoRequest(message.messageText) // deserialize messageText to EchoRequest
          )
        // add more service IDs here
      }
    }

  def validateLocalMessage(localMessage: LocalMessage): MessageValidationResult[ValidatedLocalMessage] = {
    localMessage.validate
  }

  private def validateBusinessServiceRequest(
    businessServiceRequest: MessageValidationResult[(BusinessService,BusinessServiceRequest)]
  ): RequestValidationResult[BusinessServiceRequest] =
    businessServiceRequest.map { request =>
      request._1 match {
        case value: RequestValidation =>
          value.validateRequest(request._2)
        case _ =>
          businessServiceRequest.validNec
      }
    }


  def validate: MessageValidationResult[ValidatedLocalMessage] = {
    (persistentMessageId.validNec, validateServiceId, validateMessageText, validateMessageId).mapN(ValidatedLocalMessage)
  }

  // }

  private def execBusinessService[T <: BusinessServiceRequest, U <: BusinessServiceResponse, E <: ServiceError](
    businessService: BusinessService,
    validatedBusinessServiceRequest: ValidatedNec[RequestValidationError, T]
  ): IO[ValidatedNec[RequestValidationError, Either[ServiceError, BusinessServiceResponse]]] =
    IO.delay(validatedBusinessServiceRequest.map(businessService.process))

  private def sendResultToJmsQueue[U <: BusinessServiceResponse, E <: ServiceError](
    businessResult: ValidatedNec[RequestValidationError, Either[E, U]],
    requestMessage: ValidatedLocalMessage
  ): IO[Unit] = {
    val replyMessage: String =
      businessResult match {
        case Valid(Right(businessResult)) =>
          requestMessage.serviceId match {
            case ECHO001 => businessResult.content
            // TODO(RW) add more services here
          }

        case Valid(Left(serviceError)) =>
          s"""{status: "SERVICE-ERROR", reference: "$getCustomerErrorReference", code: "${serviceError.code}", message: "${serviceError.message}"}"""

        case Invalid(requestValidationFailures) =>
          val text = requestValidationFailures.reduceLeftTo(_.message)((acc, cur) => acc + cur.message)
          s"""{status: "INVALID-REQUEST", reference: "$getCustomerErrorReference", message: "$text"}"""
      }
    localProducer.send(replyMessage, requestMessage)
  } // TODO(RW) add recoverWith here to handle unexpected exceptions and send a message back to the client

  private def getCustomerErrorReference: UUID = generator.generate()

}
