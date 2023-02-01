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
import uk.gov.nationalarchives.omega.api.business._
import uk.gov.nationalarchives.omega.api.business.echo.{ EchoRequest, EchoResponse, EchoService }
import uk.gov.nationalarchives.omega.api.services.ServiceIdentifier.ECHO001

import java.util.UUID

class Dispatcher(val localProducer: LocalProducer) {

  def run(dispatcherId: Int)(q: Queue[IO, LocalMessage]): IO[Unit] =
    for {
      requestMessage <- q.take
      _ <- IO.delay {
             println(s"Dispatcher # $dispatcherId, processing message id: ${requestMessage.persistentMessageId}")
           }
      serviceRequest                  <- createServiceRequest(requestMessage)
      businessService                 <- IO.pure(serviceRequest._1)
      businessServiceRequest          <- IO.pure(serviceRequest._2)
      validatedBusinessServiceRequest <- validateBusinessServiceRequest(businessService, businessServiceRequest)
      businessResult                  <- execBusinessService(businessService, validatedBusinessServiceRequest)
      res                             <- sendResultToJmsQueue(businessResult, requestMessage)
    } yield res

  private def createServiceRequest(localMessage: LocalMessage): IO[(BusinessService, BusinessServiceRequest)] =
    localMessage.serviceId match {
      case ECHO001 =>
        IO.delay {
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
  ): IO[ValidatedNec[RequestValidationError, BusinessServiceRequest]] =
    IO.delay {
      businessService match {
        case value: RequestValidation =>
          value.validateRequest(businessServiceRequest)
        case _ =>
          Validated.valid(businessServiceRequest)
      }
    }

  private def execBusinessService[T <: BusinessServiceRequest, U <: BusinessServiceResponse, E <: BusinessServiceError](
    businessService: BusinessService,
    validatedBusinessServiceRequest: ValidatedNec[RequestValidationError, T]
  ): IO[ValidatedNec[RequestValidationError, Either[BusinessServiceError, BusinessServiceResponse]]] =
    IO.delay {
      validatedBusinessServiceRequest.map(businessService.process)
    }

  private def sendResultToJmsQueue[U <: BusinessServiceResponse, E <: BusinessServiceError](
    businessResult: ValidatedNec[RequestValidationError, Either[E, U]],
    requestMessage: LocalMessage
  ): IO[Unit] = {
    val replyMessage: IO[String] = IO.delay {
      businessResult match {
        case Valid(Right(businessResult)) =>
          requestMessage.serviceId match {
            case ECHO001 => businessResult.asInstanceOf[EchoResponse].text
            // add more services here
          }

        case Valid(Left(serviceError)) =>
          val customerErrorReference = UUID.randomUUID()
          s"""{status: "SERVICE-ERROR", reference: "$customerErrorReference", code: "${serviceError.code}", message: "${serviceError.message}"}"""

        case Invalid(requestValidationFailures) =>
          val customerErrorReference = UUID.randomUUID()
          s"""{status: "INVALID-REQUEST", reference: "$customerErrorReference", message: "$requestValidationFailures"}"""
      }
    }
    localProducer.send(replyMessage, requestMessage)
  }

}
