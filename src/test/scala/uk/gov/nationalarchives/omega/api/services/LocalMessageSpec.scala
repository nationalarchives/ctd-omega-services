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
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{ InvalidApplicationID, InvalidAuthToken, InvalidJMSMessageID, InvalidMessageFormat, InvalidMessageTypeID, InvalidReplyAddress, MissingApplicationID, MissingAuthToken, MissingJMSMessageID, MissingJMSTimestamp, MissingMessageFormat, MissingMessageTypeID, MissingReplyAddress }
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, ValidatedLocalMessage }

import java.time.{ Instant, LocalDateTime, ZoneOffset }
import java.util.UUID

class LocalMessageSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val validPersistentMessageId = Version1UUID.generate()
  private val validCorrelationId = UUID.randomUUID().toString
  private val validMessageText = "At vero eos et accusamus et iusto odio dignissimos"
  private val validApplicationId = "ABCD002"
  private val validMessageTypeId = "OSGESZZZ100"
  private val validEpochTimeInMilliseconds = System.currentTimeMillis()
  private val validMessageFormat = "application/json"
  private val validAuthToken = "AbCdEf123456"
  private val validReplyAddress = "ABCD002.a"
  private val validServiceId = "OSGESZZZ100"
  lazy private val validLocalMessage =
    LocalMessage(
      persistentMessageId = validPersistentMessageId,
      messageText = validMessageText,
      omgMessageTypeId = Some(validServiceId),
      jmsMessageId = Some(validCorrelationId),
      omgApplicationId = Some(validApplicationId),
      jmsTimestamp = Some(validEpochTimeInMilliseconds),
      omgMessageFormat = Some(validMessageFormat),
      omgToken = Some(validAuthToken),
      omgReplyAddress = Some(validReplyAddress)
    )

  "A LocalMessage when" - {
    "validated when" - {
      "all fields are provided and valid" in {

        val localMessage = validLocalMessage

        val applicationId = localMessage.validateOmgApplicationId
        val omgToken = localMessage.validateOmgToken
        val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

        validationResult mustBe Validated.Valid(
          ValidatedLocalMessage(
            jmsMessageId = validCorrelationId,
            messageText = validMessageText,
            persistentMessageId = validPersistentMessageId,
            omgMessageTypeId = validMessageTypeId,
            omgApplicationId = validApplicationId,
            jmsLocalDateTime =
              LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
            omgMessageFormat = validMessageFormat,
            authToken = validAuthToken,
            omgReplyAddress = validReplyAddress
          )
        )

      }
      "a single field is missing or invalid, specifically" - {
        "the correlation ID" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(jmsMessageId = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingJMSMessageID))

          }
          "is empty, without padding" in {

            val localMessage = validLocalMessage.copy(jmsMessageId = Some(""))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidJMSMessageID))

          }
          "is empty, with padding" in {

            val localMessage = validLocalMessage.copy(jmsMessageId = Some("        "))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidJMSMessageID))

          }
        }
        "the service ID" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(omgMessageTypeId = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingMessageTypeID))

          }
          "is blank" in {

            val localMessage = validLocalMessage.copy(omgMessageTypeId = Some(""))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidMessageTypeID))

          }
          "isn't compliant with the expected pattern" in {

            val localMessage = validLocalMessage.copy(omgMessageTypeId = Some("ECHO001"))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidMessageTypeID))

          }
          "is compliant with the expected pattern, but is not recognised." in {

            val unknownMessageTypeId = "OSGESXXX100"

            val localMessage = validLocalMessage.copy(omgMessageTypeId = Some(unknownMessageTypeId))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                jmsMessageId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                omgMessageTypeId = unknownMessageTypeId,
                omgApplicationId = validApplicationId,
                jmsLocalDateTime =
                  LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                omgMessageFormat = "application/json",
                authToken = validAuthToken,
                omgReplyAddress = validReplyAddress
              )
            )

          }
        }
        "the application ID" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(omgApplicationId = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingApplicationID, InvalidReplyAddress))

          }
          "is invalid" in {

            val localMessage = validLocalMessage.copy(omgApplicationId = Some("ABC123"))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidApplicationID, InvalidReplyAddress))

          }
        }
        "the timestamp" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(jmsTimestamp = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingJMSTimestamp))

          }
        }
        "the message format" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(omgMessageFormat = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingMessageFormat))

          }
          "is invalid" in {

            val localMessage = validLocalMessage.copy(omgMessageFormat = Some("text/plain"))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidMessageFormat))

          }
          "is valid (but with padding)" in {

            val localMessage = validLocalMessage.copy(omgMessageFormat = Some("  application/json  "))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                jmsMessageId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                omgMessageTypeId = validMessageTypeId,
                omgApplicationId = validApplicationId,
                jmsLocalDateTime =
                  LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                omgMessageFormat = "application/json",
                authToken = validAuthToken,
                omgReplyAddress = validReplyAddress
              )
            )

          }
          "is valid (but with the wrong casing)" in {

            val localMessage = validLocalMessage.copy(omgMessageFormat = Some("APPLICATION/JSON"))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                jmsMessageId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                omgMessageTypeId = validMessageTypeId,
                omgApplicationId = validApplicationId,
                jmsLocalDateTime =
                  LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                omgMessageFormat = "application/json",
                authToken = validAuthToken,
                omgReplyAddress = validReplyAddress
              )
            )

          }
        }
        "the auth token" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(omgToken = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingAuthToken))

          }
          "is blank" in {

            val localMessage = validLocalMessage.copy(omgToken = Some(""))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidAuthToken))

          }
          "is blank (but with padding)" in {

            val localMessage = validLocalMessage.copy(omgToken = Some("   "))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidAuthToken))

          }
          "is valid (but with the wrong casing)" in {

            val localMessage = validLocalMessage.copy(omgMessageFormat = Some("APPLICATION/JSON"))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Validated.Valid(
              ValidatedLocalMessage(
                jmsMessageId = validCorrelationId,
                messageText = validMessageText,
                persistentMessageId = validPersistentMessageId,
                omgMessageTypeId = validMessageTypeId,
                omgApplicationId = validApplicationId,
                jmsLocalDateTime =
                  LocalDateTime.ofInstant(Instant.ofEpochMilli(validEpochTimeInMilliseconds), ZoneOffset.UTC),
                omgMessageFormat = "application/json",
                authToken = validAuthToken,
                omgReplyAddress = validReplyAddress
              )
            )

          }
        }
        "the reply address" - {
          "is missing" in {

            val localMessage = validLocalMessage.copy(omgReplyAddress = None)

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(MissingReplyAddress))

          }
          "is blank" in {

            val localMessage = validLocalMessage.copy(omgReplyAddress = Some(""))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidReplyAddress))

          }
          "is invalid but contains the application ID" in {

            val localMessage = validLocalMessage.copy(omgReplyAddress = Some("ABCD002."))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidReplyAddress))

          }
          "is valid (pattern-wise) but doesn't contain the application ID" in {

            val localMessage = validLocalMessage.copy(omgReplyAddress = Some("XXXX002.a"))

            val applicationId = localMessage.validateOmgApplicationId
            val omgToken = localMessage.validateOmgToken
            val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

            validationResult mustBe Invalid(Chain(InvalidReplyAddress))

          }
        }
      }
      "multiple fields are missing or invalid" in {

        val localMessage = validLocalMessage.copy(jmsMessageId = Some(""), omgMessageTypeId = None)

        val applicationId = localMessage.validateOmgApplicationId
        val omgToken = localMessage.validateOmgToken
        val validationResult = localMessage.validateOtherHeaders(omgToken, applicationId)

        validationResult mustBe Invalid(Chain(InvalidJMSMessageID, MissingMessageTypeID))

      }
    }
  }

}
