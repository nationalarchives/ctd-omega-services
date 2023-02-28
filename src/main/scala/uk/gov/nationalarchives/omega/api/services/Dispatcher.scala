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
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.business.echo.{ EchoRequest, EchoResponse, EchoService }
import uk.gov.nationalarchives.omega.api.services.ServiceIdentifier.ECHO001

import java.util.UUID

class Dispatcher(val localProducer: LocalProducer) {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  def run(dispatcherId: Int)(q: Queue[IO, LocalMessage]): IO[Unit] =
    for {
      requestMessage <- q.take
      _ <- logger.info(s"Dispatcher # $dispatcherId, processing message id: ${requestMessage.persistentMessageId}")
      (businessService, businessServiceRequest) <- createServiceRequest(requestMessage)
      validatedBusinessServiceRequest <-
        IO.pure(validateBusinessServiceRequest(businessService, businessServiceRequest))
      businessResult <- execBusinessService(businessService, validatedBusinessServiceRequest)
      res            <- sendResultToJmsQueue(businessResult, requestMessage)
    } yield res

  private def createServiceRequest(localMessage: LocalMessage): IO[(BusinessService, BusinessServiceRequest)] =
    localMessage.serviceId match {
      case ECHO001 =>
        IO.pure {
          Tuple2(
            new EchoService(),
            EchoRequest(localMessage.messageText)
          )
        }
      // add more service IDs here
    }

  private def validateBusinessServiceRequest(
    businessService: BusinessService,
    businessServiceRequest: BusinessServiceRequest
  ): ValidatedNec[RequestValidationError, BusinessServiceRequest] =
    businessService match {
      case value: RequestValidation =>
        value.validateRequest(businessServiceRequest)
      case _ =>
        Validated.valid(businessServiceRequest)
    }
  // }

  private def execBusinessService[T <: BusinessServiceRequest, U <: BusinessServiceResponse, E <: BusinessServiceError](
    businessService: BusinessService,
    validatedBusinessServiceRequest: ValidatedNec[RequestValidationError, T]
  ): IO[ValidatedNec[RequestValidationError, Either[BusinessServiceError, BusinessServiceResponse]]] =
    IO.delay(validatedBusinessServiceRequest.map(businessService.process))

  private def sendResultToJmsQueue[U <: BusinessServiceResponse, E <: BusinessServiceError](
    businessResult: ValidatedNec[RequestValidationError, Either[E, U]],
    requestMessage: LocalMessage
  ): IO[Unit] = {
    val replyMessage: String =
      businessResult match {
        case Valid(Right(businessResult)) =>
          requestMessage.serviceId match {
            case ECHO001 => businessResult.content
            // add more services here
          }

        case Valid(Left(serviceError)) =>
          s"""{status: "SERVICE-ERROR", reference: "$getCustomerErrorReference", code: "${serviceError.code}", message: "${serviceError.message}"}"""

        case Invalid(requestValidationFailures) =>
          val text = requestValidationFailures.reduceLeftTo(_.message)((acc,cur) => acc + cur.message)
          s"""{status: "INVALID-REQUEST", reference: "$getCustomerErrorReference", message: "$text"}"""
      }
    localProducer.send(replyMessage, requestMessage)
  }

  private def getCustomerErrorReference: UUID = UUID.randomUUID()

}
