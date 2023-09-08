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

package uk.gov.nationalarchives.omega.api.business.records

import cats.data.Validated
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import uk.gov.nationalarchives.omega.api.business.{BusinessRequestValidation, BusinessService, BusinessServiceError, BusinessServiceReply}
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{InvalidMessagePayload, MessageValidationError, ValidationResult}
import uk.gov.nationalarchives.omega.api.messages.reply._
import uk.gov.nationalarchives.omega.api.messages.request.{ RequestByIdentifier, RequestMessage}
import uk.gov.nationalarchives.omega.api.messages._
import uk.gov.nationalarchives.omega.api.repository.model._
import uk.gov.nationalarchives.omega.api.repository.{AbstractRepository, BaseURL}

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class GetRecordService(val repository: AbstractRepository) extends BusinessService with BusinessRequestValidation {

  // TODO we need to validate that the message contains a valid record concept identifier
  override def validateRequest(validatedLocalMessage: ValidatedLocalMessage): ValidationResult[RequestMessage] = {
    val recordConceptUriPattern: Regex = (BaseURL.cat + "/[A-Z]{1,4}.[0-9]{3,4}.[0-9A-Z]{4}.(P|D)$").r
    if (validatedLocalMessage.messageText.nonEmpty) {
      decode[RequestByIdentifier](validatedLocalMessage.messageText) match {
        case Right(request) => if(request.identifier.nonEmpty && recordConceptUriPattern.matches(request.identifier)){
          Validated.valid(request)
        } else {
          Validated.invalidNec[MessageValidationError, RequestMessage](
            InvalidMessagePayload(message = Some("Missing identifier value"))
          )
        }
        case Left(error) =>
          Validated.invalidNec[MessageValidationError, RequestMessage](
            InvalidMessagePayload(cause = Some(error))
          )
      }
    } else {
      Validated.invalidNec[MessageValidationError, RequestMessage](
        InvalidMessagePayload(message = Some("Missing identifier"))
      )
    }
  }

  override def process(request: RequestMessage): Either[BusinessServiceError, BusinessServiceReply] = {
    val record = for {
      getRecordRequest <- Try(request.asInstanceOf[RequestByIdentifier])
      recordFull       <- getRecord(getRecordRequest)
    } yield recordFull
    record match {
      case Success(rec)       => Right(GetRecordReply(rec.asJson.toString()))
      case Failure(exception) => Left(GetRecordError(exception.getMessage))
    }
  }

  private def getRecord(recordRequest: RequestByIdentifier): Try[RecordFull] = {
    val recordConceptUri = recordRequest.identifier
    val recordConceptId = recordConceptUri.substring(recordConceptUri.lastIndexOf('/') + 1)
    for {
      recordConceptEntities       <- repository.getRecordConceptEntity(recordConceptId)
      creatorEntities             <- repository.getCreatorEntities(recordConceptEntities.head.recordConceptUri.toString)
      recordDescriptionSummaries  <- repository.getRecordDescriptionSummaries(recordConceptUri)
      recordDescriptionProperties <- repository.getRecordDescriptionProperties(recordConceptUri)
      accessRights                <- repository.getAccessRights(recordConceptUri)
      isPartOf                    <- repository.getIsPartOf(recordConceptUri)
      secondaryIdentifiers        <- repository.getSecondaryIdentifiers(recordConceptUri)
      isReferencedBy                <- repository.getIsReferencedBy(recordConceptUri)
      recordFull <- convertToRecordFull(
                      recordConceptEntities.head,
                      creatorEntities,
                      recordDescriptionSummaries,
                      recordDescriptionProperties,
                      accessRights,
                      isPartOf,
                      secondaryIdentifiers,
        isReferencedBy
                    )
    } yield recordFull
  }

  private def convertToRecordFull(
    recordConceptEntity: RecordConceptEntity,
    creatorEntities: List[CreatorEntity],
    recordSummaryEntities: List[RecordDescriptionSummaryEntity],
    recordPropertiesEntities: List[RecordDescriptionPropertiesEntity],
    accessRightsEntities: List[AccessRightsEntity],
    isPartOfEntities: List[IsPartOfEntity],
    secondaryIdentifierEntities: List[SecondaryIdentifierEntity],
    isReferencedBy: List[LabelledIdentifierEntity]
  ): Try[RecordFull] =
    for {
      recordType <- RecordType.fromUri(recordConceptEntity.formatUri.toString)
    } yield RecordFull(
      recordConceptEntity.recordConceptUri.toString,
      recordType,
      creatorEntities.map(ce => ce.identifier.toString),
      recordConceptEntity.currentDescriptionUri.toString,
      getRecordDescriptionFull(
        recordSummaryEntities,
        recordPropertiesEntities,
        accessRightsEntities,
        isPartOfEntities,
        secondaryIdentifierEntities,
        isReferencedBy
      )
    ) // TO BE CONTINUED - write a test for this!

  private def getRecordDescriptionFull(
    recordSummaryEntities: List[RecordDescriptionSummaryEntity],
    recordPropertiesEntities: List[RecordDescriptionPropertiesEntity],
    accessRightsEntities: List[AccessRightsEntity],
    isPartOfEntities: List[IsPartOfEntity],
    secondaryIdentifierEntities: List[SecondaryIdentifierEntity],
    isReferencedByEntities: List[LabelledIdentifierEntity]
  ): List[RecordDescriptionFull] = {
    val accessRightsMap = getAccessRightsMap(accessRightsEntities)
    val isPartOfMap: Map[String, List[String]] = isPartOfEntities
      .groupBy(_.recordDescriptionUri)
      .map(entry => entry._1.toString -> entry._2.map(a => a.recordSetConceptUri.toString))
    val secondaryIdentifierMap: Map[String, List[TypedIdentifier]] = secondaryIdentifierEntities
      .groupBy(_.recordDescriptionUri)
      .map(entry =>
        entry._1.toString -> entry._2.map(a => TypedIdentifier(a.secondaryIdentifier, a.identifierProperty.toString))
      )
    val isReferencedByMap: Map[String, List[LabelledIdentifier]] = isReferencedByEntities
      .groupBy(_.recordDescriptionUri)
      .map(entry => entry._1.toString -> entry._2.map(a => LabelledIdentifier(a.identifier.toString, a.label)
      ))
    recordSummaryEntities.map(summary =>
      RecordDescriptionFull(
        summary = RecordDescriptionSummary(
          identifier = summary.recordDescriptionUri.toString,
          secondaryIdentifier = secondaryIdentifierMap.get(summary.recordDescriptionUri.toString),
          label = summary.scopeAndContent,
          description = summary.scopeAndContent,
          accessRights = accessRightsMap.getOrElse(summary.recordDescriptionUri.toString, List.empty),
          isPartOf = isPartOfMap.getOrElse(summary.recordDescriptionUri.toString, List.empty),
          previousSibling = summary.previousSiblingUri.flatMap(uri => Some(uri.toString)),
          versionTimestamp = summary.versionTimestamp,
          previousDescription = summary.previousDescriptionUri.flatMap(uri => Some(uri.toString))
        ),
        properties = getRecordPropertiesEntity(recordPropertiesEntities, summary.recordDescriptionUri.toString, isReferencedByMap)
      )
    )
  }

  private def getAccessRightsMap(accessRightsEntities: List[AccessRightsEntity]): Map[String, List[String]] = {
    val groupedMap: Map[String, List[AccessRightsEntity]] =
      accessRightsEntities.groupBy(_.recordDescriptionUri.toString)
    groupedMap.map(entry => entry._1 -> entry._2.map(a => a.accessRights.toString))
  }

  private def getRecordPropertiesEntity(
    recordProperties: List[RecordDescriptionPropertiesEntity],
    descriptionUri: String, // TODO shouldn't be needed - refactor
    isReferencedByMap: Map[String, List[LabelledIdentifier]]
  ): RecordDescriptionProperties = {
    val recordPropertiesMap: Map[String, List[RecordDescriptionPropertiesEntity]] =
      recordProperties.groupBy(_.recordDescriptionUri.toString)
    val recordDescriptionProperties: List[RecordDescriptionPropertiesEntity] =
      recordPropertiesMap.getOrElse(descriptionUri, List.empty)
    recordDescriptionProperties.headOption match {
      case Some(props) => propertiesFromEntity(props, isReferencedByMap)
      case None        => RecordDescriptionProperties()
    }
  }

  private def propertiesFromEntity(entity: RecordDescriptionPropertiesEntity, isReferencedByMap: Map[String, List[LabelledIdentifier]]): RecordDescriptionProperties =
    RecordDescriptionProperties(
      assetLegalStatus = getLegalStatus(entity),
      legacyTnaCs13RecordType = getCS13RecordType(entity),
      designationOfEdition = entity.designationOfEdition,
      created = getCreated(entity),
      archivistsNote = entity.archivistsNote,
      sourceOfAcquisition = entity.sourceOfAcquisition.flatMap(uri => Some(uri.toString)),
      custodialHistory = entity.custodialHistory,
      administrativeBiographicalBackground = entity.adminBiogBackground,
      accumulation = getAccumulation(entity),
      appraisal = entity.appraisal,
      accrualPolicy = entity.accrualPolicy.flatMap(uri => Some(uri.toString)),
      layout = entity.layout,
      publicationNote = entity.publicationNote,
      isReferencedBy = isReferencedByMap.get(entity.recordDescriptionUri.toString)
    )

  private def getLegalStatus(entity: RecordDescriptionPropertiesEntity): Option[LabelledIdentifier] =
    for {
      legalStatusUri   <- entity.assetLegalStatus
      legalStatusLabel <- entity.legalStatusLabel
    } yield LabelledIdentifier(legalStatusUri.toString, legalStatusLabel)

  private def getCS13RecordType(entity: RecordDescriptionPropertiesEntity): Option[CS13RecordType] =
    entity.legacyType.flatMap { uri =>
      CS13RecordType.fromUri(uri) match {
        case Success(recordType) => Some(recordType)
        case _                   => None // TODO (RW) log error here (see PACT-1071)
      }
    }

  private def getCreated(entity: RecordDescriptionPropertiesEntity): Option[DescribedTemporal] =
    entity.createdType.flatMap(uri =>
      uri.toString match {
        case s"${BaseURL.time}ProperInterval" =>
          for {
            description <- entity.createdDescription
            dateFrom    <- entity.createdBeginning
            dateTo      <- entity.createdEnd
          } yield DescribedTemporal(description, TemporalInterval(dateFrom, dateTo))
        case s"${BaseURL.time}Instant" =>
          for {
            description <- entity.createdDescription
            instant     <- entity.createdInstant
          } yield DescribedTemporal(description, TemporalInstant(instant))
        case _ => None // TODO (RW) log error here (see PACT-1071)
      }
    )

  // TODO (RW) refactor this and getCreated (DRY)
  private def getAccumulation(entity: RecordDescriptionPropertiesEntity): Option[DescribedTemporal] =
    entity.accumulationType.flatMap(uri =>
      uri.toString match {
        case s"${BaseURL.time}ProperInterval" =>
          for {
            description <- entity.accumulationDescription
            dateFrom    <- entity.accumulationBeginning
            dateTo      <- entity.accumulationEnd
          } yield DescribedTemporal(description, TemporalInterval(dateFrom, dateTo))
        case s"${BaseURL.time}Instant" =>
          for {
            description <- entity.accumulationDescription
            instant     <- entity.accumulationInstant
          } yield DescribedTemporal(description, TemporalInstant(instant))
        case _ => None // TODO (RW) log error here (see PACT-1071)
      }
    )

}
