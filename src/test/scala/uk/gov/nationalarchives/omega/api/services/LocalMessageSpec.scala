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

import cats.data.Validated.Invalid
import cats.data.{ Chain, Validated }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.services.LocalMessage.{ InvalidApplicationID, InvalidAuthToken, InvalidJMSMessageID, InvalidMessageFormat, InvalidResponseAddress, InvalidServiceID, MissingApplicationID, MissingAuthToken, MissingJMSMessageID, MissingJMSTimestamp, MissingMessageFormat, MissingResponseAddress, MissingServiceID }

import java.time.{ Instant, LocalDateTime, ZoneOffset }
import java.util.UUID

class LocalMessageSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val validPersistentMessageId = Version1UUID.generate()
  private val validCorrelationId = UUID.randomUUID().toString
  private val validMessageText = "At vero eos et accusamus et iusto odio dignissimos"
  private val validApplicationId = "ABCD002"
  private val validEpochTimeInMilliseconds = System.currentTimeMillis()
  private val validMessageFormat = "application/json"
  private val validAuthToken = "AbCdEf123456"
  private val validResponseAddress = "ABCD002.a"
  private val validServiceId = "OSGESZZZ100"
  lazy private val validLocalMessage =
    LocalMessage(
      persistentMessageId = validPersistentMessageId,
      messageText = validMessageText,
      serviceId = Some(validServiceId),
      correlationId = Some(validCorrelationId),
      applicationId = Some(validApplicationId),
      epochTimeInMilliseconds = Some(validEpochTimeInMilliseconds),
      messageFormat = Some(validMessageFormat),
      authToken = Some(validAuthToken),
      responseAddress = Some(validResponseAddress)
    )

  "A LocalMessage when" - {
    "validated when" - {
      "all fields are provided and valid" in {

        val localMessage = validLocalMessage

        val validationResult = localMessage.validate()

        validationResult mustBe Validated.Valid(
          ValidatedLocalMessage(
            correlationId = validCorrelationId,
            messageText = validMessageText,
            persistentMessageId = validPersistentMessageId,
            serviceId = ServiceIdentifier.ECHO001,
            applicationId = validApplicationId,
            time = LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
            messageFormat = validMessageFormat,
            authToken = validAuthToken,
            responseAddress = validResponseAddress
          )
        )

      }
      "a single field is missing or invalid, specifically" - {
        "the correlation ID" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(correlationId = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingJMSMessageID))

          }
          "is empty, without padding" in {

            val localMessage = validLocalMessage.copy(correlationId = Some(""))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidJMSMessageID))

          }
          "is empty, with padding" in {

            val localMessage = validLocalMessage.copy(correlationId = Some("        "))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidJMSMessageID))

          }
        }
        "the service ID" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(serviceId = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingServiceID))

          }
          "is blank" in {

            val localMessage = validLocalMessage.copy(serviceId = Some(""))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidServiceID))

          }
          "isn't compliant with the expected pattern" in {

            val localMessage = validLocalMessage.copy(serviceId = Some("ECHO001"))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidServiceID))

          }
          "is compliant with the expected pattern, but is not recognised." in {

            val localMessage = validLocalMessage.copy(serviceId = Some("OSGESXXX100"))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidServiceID))

          }
        }
        "the application ID" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(applicationId = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingApplicationID, InvalidResponseAddress))

          }
          "is invalid" in {

            val localMessage = validLocalMessage.copy(applicationId = Some("ABC123"))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidApplicationID, InvalidResponseAddress))

          }
        }
        "the timestamp" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(epochTimeInMilliseconds = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingJMSTimestamp))

          }
        }
        "the message format" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(messageFormat = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingMessageFormat))

          }
          "is invalid" in {

            val localMessage = validLocalMessage.copy(messageFormat = Some("text/plain"))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidMessageFormat))

          }
          "is valid (but with padding)" in {

            val localMessage = validLocalMessage.copy(messageFormat = Some("  application/json  "))

            val validationResult = localMessage.validate()

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                correlationId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                serviceId = ServiceIdentifier.ECHO001,
                applicationId = validApplicationId,
                time = LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                messageFormat = "application/json",
                authToken = validAuthToken,
                responseAddress = validResponseAddress
              )
            )

          }
          "is valid (but with the wrong casing)" in {

            val localMessage = validLocalMessage.copy(messageFormat = Some("APPLICATION/JSON"))

            val validationResult = localMessage.validate()

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                correlationId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                serviceId = ServiceIdentifier.ECHO001,
                applicationId = validApplicationId,
                time = LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                messageFormat = "application/json",
                authToken = validAuthToken,
                responseAddress = validResponseAddress
              )
            )

          }
        }
        "the auth token" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(authToken = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingAuthToken))

          }
          "is blank" in {

            val localMessage = validLocalMessage.copy(authToken = Some(""))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidAuthToken))

          }
          "is blank (but with padding)" in {

            val localMessage = validLocalMessage.copy(authToken = Some("   "))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidAuthToken))

          }
          "is valid (but with the wrong casing)" in {

            val localMessage = validLocalMessage.copy(messageFormat = Some("APPLICATION/JSON"))

            val validationResult = localMessage.validate()

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                correlationId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                serviceId = ServiceIdentifier.ECHO001,
                applicationId = validApplicationId,
                time = LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                messageFormat = "application/json",
                authToken = validAuthToken,
                responseAddress = validResponseAddress
              )
            )

          }
        }
        "the response address" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(responseAddress = None)

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(MissingResponseAddress))

          }
          "is blank" in {

            val localMessage = validLocalMessage.copy(responseAddress = Some(""))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidResponseAddress))

          }
          "is invalid but contains the application ID" in {

            val localMessage = validLocalMessage.copy(responseAddress = Some("ABCD002."))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidResponseAddress))

          }
          "is valid (pattern-wise) but doesn't contain the application ID" in {

            val localMessage = validLocalMessage.copy(responseAddress = Some("XXXX002.a"))

            val validationResult = localMessage.validate()

            validationResult mustBe Invalid(Chain(InvalidResponseAddress))

          }
        }
      }
      "multiple fields are missing or invalid" in {

        val localMessage = validLocalMessage.copy(correlationId = Some(""), serviceId = None)

        val validationResult = localMessage.validate()

        validationResult mustBe Invalid(Chain(InvalidJMSMessageID, MissingServiceID))

      }
    }
  }

}
