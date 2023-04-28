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

import cats.data.Chain
import cats.data.Validated.{ Invalid, Valid }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.{ MatchResult, Matcher }
import uk.gov.nationalarchives.omega.api.business.{ BusinessServiceError, BusinessServiceReply, BusinessServiceRequest, TextIsNonEmptyCharacters }

class EchoServiceSpec extends AnyFreeSpec with Matchers {

  val echoService = new EchoService()

  "The EchoService" - {
    "when asked to" - {
      "validate a request, when that request" - {
        "is an EchoRequest, with text which" - {
          "is empty" in {

            val echoRequest = EchoRequest("")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beInvalid("Echo Text cannot be empty.")

          }
          "only consists of whitespace" in {

            val echoRequest = EchoRequest("         ")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beInvalid("Echo Text cannot be empty.")

          }
          "has only one character" in {

            val echoRequest = EchoRequest("x")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beValid(echoRequest)

          }
          "has several characters" in {

            val echoRequest = EchoRequest("Hello, world")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beValid(echoRequest)

          }
          "has many characters" in {

            val echoRequest = EchoRequest("Hello, world" * 100)

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beValid(echoRequest)

          }
        }
        "is *not* an EchoRequest, with text which" - {
          "is empty" in {

            val echoRequest = NotAnEchoRequest("")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beInvalid("Echo Text cannot be empty.")

          }
          "only consists of whitespace" in {

            val echoRequest = NotAnEchoRequest("         ")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beInvalid("Echo Text cannot be empty.")

          }
          "has only one character" in {

            val echoRequest = NotAnEchoRequest("x")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beValid(echoRequest)

          }
          "has several characters" in {

            val echoRequest = NotAnEchoRequest("Hello, world")

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beValid(echoRequest)

          }
          "has many characters" in {

            val echoRequest = NotAnEchoRequest("Hello, world" * 100)

            val validationResult = echoService.validateRequest(echoRequest)

            validationResult must beValid(echoRequest)

          }
        }
      }
      "process a request, when that request" - {
        "is an EchoRequest which" - {
          "contains the text 'error', when" - {
            "upper case" in {

              val echoRequest = EchoRequest("ERROR: Some details about that error.")

              val result = echoService.process(echoRequest)

              result must beAFailure("Explicit error: ERROR: Some details about that error.")

            }
            "lower case" in {

              val echoRequest = EchoRequest("error: Some details about that error.")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says: error: Some details about that error.")

            }
          }
          "does not contain the text 'error', when that text" - {
            "is empty" in {

              val echoRequest = EchoRequest("")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says: ")

            }
            "only consists of whitespace" in {

              val echoRequest = EchoRequest("          ")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says:           ")

            }
            "has several characters" in {

              val echoRequest = EchoRequest("Hello, world")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says: Hello, world")

            }
          }
        }
        "is *not* an EchoRequest which" - {
          "contains the text 'error', when" - {
            "upper case" in {

              val echoRequest = NotAnEchoRequest("ERROR: Some details about that error.")

              val result = echoService.process(echoRequest)

              result must beAFailure("Explicit error: ERROR: Some details about that error.")

            }
            "lower case" in {

              val echoRequest = NotAnEchoRequest("error: Some details about that error.")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says: error: Some details about that error.")

            }
          }
          "does not contain the text 'error', when that text" - {
            "is empty" in {

              val echoRequest = NotAnEchoRequest("")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says: ")

            }
            "only consists of whitespace" in {

              val echoRequest = NotAnEchoRequest("          ")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says:           ")

            }
            "has several characters" in {

              val echoRequest = NotAnEchoRequest("Hello, world")

              val result = echoService.process(echoRequest)

              result must beASuccess("The Echo Service says: Hello, world")

            }
          }
        }
      }

    }
  }

  private case class NotAnEchoRequest(text: String) extends BusinessServiceRequest

  def beValid(expectedBusinessServiceRequest: BusinessServiceRequest): Matcher[echoService.ValidationResult] =
    (validationResult: echoService.ValidationResult) =>
      MatchResult(
        validationResult == Valid(expectedBusinessServiceRequest),
        s"We expected the validation result to be valid - returning the business service request [$expectedBusinessServiceRequest] - but it was actually [$validationResult].",
        s"We didn't expect the validation result to be valid - returning the business service request [$expectedBusinessServiceRequest] - but it was."
      )

  def beInvalid(expectedErrorMessage: String): Matcher[echoService.ValidationResult] =
    (validationResult: echoService.ValidationResult) =>
      MatchResult(
        validationResult == Invalid(Chain(TextIsNonEmptyCharacters(expectedErrorMessage))),
        s"We expected the validation result to be invalid - with the error message [$expectedErrorMessage] - but it was actually [$validationResult].",
        s"We didn't expect the validation result to be invalid - with the error message [$expectedErrorMessage] - but it was."
      )

  def beASuccess(expectedMessage: String): Matcher[Either[BusinessServiceError, BusinessServiceReply]] =
    (processingResult: Either[BusinessServiceError, BusinessServiceReply]) =>
      MatchResult(
        processingResult == Right(EchoReply(expectedMessage)),
        s"We expected the processing result to be successful, with the message [$expectedMessage], but it was actually [$processingResult].",
        s"We didn't expect the processing result to be successful, with the message [$expectedMessage], but it was."
      )

  def beAFailure(expectedErrorMessage: String): Matcher[Either[BusinessServiceError, BusinessServiceReply]] =
    (processingResult: Either[BusinessServiceError, BusinessServiceReply]) =>
      MatchResult(
        processingResult == Left(EchoExplicitError(expectedErrorMessage)),
        s"We expected the processing result to be a failure, with the error message [$expectedErrorMessage], but it was actually [$processingResult].",
        s"We didn't expect the processing result to be a failure, with the error message [$expectedErrorMessage], but it was."
      )

}
