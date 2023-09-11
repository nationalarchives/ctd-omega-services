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
import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.business.{ BusinessRequestValidation, BusinessService, BusinessServiceError, BusinessServiceReply }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.{ InvalidMessagePayload, MessageValidationError, ValidationResult }
import uk.gov.nationalarchives.omega.api.messages.reply._
import uk.gov.nationalarchives.omega.api.messages.request.{ RequestByIdentifier, RequestMessage }
import uk.gov.nationalarchives.omega.api.messages._
import uk.gov.nationalarchives.omega.api.repository.model._
import uk.gov.nationalarchives.omega.api.repository.{ AbstractRepository, BaseURL }

import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.{ Failure, Success, Try }

class GetRecordService(val repository: AbstractRepository) extends BusinessService with BusinessRequestValidation {

  implicit def uriToString(uri: URI): String = uri.toString

  override def validateRequest(validatedLocalMessage: ValidatedLocalMessage): ValidationResult[RequestMessage] = {
    val recordConceptUriPattern: Regex = (BaseURL.cat + "/[A-Z]{1,4}.[0-9]{3,4}.[0-9A-Z]{4}.(P|D)$").r
    if (validatedLocalMessage.messageText.nonEmpty) {
      decode[RequestByIdentifier](validatedLocalMessage.messageText) match {
        case Right(request) =>
          if (request.identifier.nonEmpty && recordConceptUriPattern.matches(request.identifier)) {
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
      creatorEntities             <- repository.getCreatorEntities(recordConceptEntities.head.recordConceptUri)
      recordDescriptionSummaries  <- repository.getRecordDescriptionSummaries(recordConceptUri)
      recordDescriptionProperties <- repository.getRecordDescriptionProperties(recordConceptUri)
      accessRights                <- repository.getAccessRights(recordConceptUri)
      isPartOf                    <- repository.getIsPartOf(recordConceptUri)
      secondaryIdentifiers        <- repository.getSecondaryIdentifiers(recordConceptUri)
      isReferencedBys             <- repository.getIsReferencedBys(recordConceptUri)
      relatedTos                  <- repository.getRelatedTos(recordConceptUri)
      separatedFroms              <- repository.getSeparatedFroms(recordConceptUri)
      uriSubjects                 <- repository.getUriSubjects(recordConceptUri)
      labelledSubjects            <- repository.getLabelledSubjects(recordConceptUri)
      recordFull <- convertToRecordFull(
                      recordConceptEntities.head,
                      creatorEntities,
                      recordDescriptionSummaries,
                      recordDescriptionProperties,
                      accessRights,
                      isPartOf,
                      secondaryIdentifiers,
                      isReferencedBys,
                      relatedTos,
                      separatedFroms,
                      uriSubjects,
                      labelledSubjects
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
    isReferencedBys: List[LabelledIdentifierEntity],
    relatedTos: List[LabelledIdentifierEntity],
    separatedFroms: List[LabelledIdentifierEntity],
    uriSubjects: List[IdentifierEntity],
    labelledSubjects: List[LabelledIdentifierEntity]
  ): Try[RecordFull] =
    for {
      recordType <- RecordType.fromUri(recordConceptEntity.formatUri)
    } yield RecordFull(
      GeneralIdentifier.fromUri(recordConceptEntity.recordConceptUri),
      recordType,
      creatorEntities.map(ce => ce.identifier),
      GeneralIdentifier.fromUri(recordConceptEntity.currentDescriptionUri),
      getRecordDescriptionFull(
        recordSummaryEntities,
        recordPropertiesEntities,
        accessRightsEntities,
        isPartOfEntities,
        secondaryIdentifierEntities,
        isReferencedBys,
        relatedTos,
        separatedFroms,
        uriSubjects,
        labelledSubjects
      )
    )

  private def getRecordDescriptionFull(
    recordSummaryEntities: List[RecordDescriptionSummaryEntity],
    recordPropertiesEntities: List[RecordDescriptionPropertiesEntity],
    accessRightsEntities: List[AccessRightsEntity],
    isPartOfEntities: List[IsPartOfEntity],
    secondaryIdentifierEntities: List[SecondaryIdentifierEntity],
    isReferencedByEntities: List[LabelledIdentifierEntity],
    relatedToEntities: List[LabelledIdentifierEntity],
    separatedFromEntities: List[LabelledIdentifierEntity],
    uriSubjectEntities: List[IdentifierEntity],
    labelledSubjectEntities: List[LabelledIdentifierEntity]
  ): List[RecordDescriptionFull] = {
    val accessRightsMap = getAccessRightsMap(accessRightsEntities)
    val isPartOfMap = getIsPartOfMap(isPartOfEntities)
    val secondaryIdentifierMap = getSecondaryIdentifiersMap(secondaryIdentifierEntities)
    val isReferencedByMap = getLabelledIdentifierMap(isReferencedByEntities)
    val relatedToMap = getLabelledIdentifierMap(relatedToEntities)
    val separatedFromMap = getLabelledIdentifierMap(separatedFromEntities)
    val subjectMap = getSubjectMap(uriSubjectEntities, labelledSubjectEntities)
    recordSummaryEntities.map(summary =>
      RecordDescriptionFull(
        summary = RecordDescriptionSummary(
          identifier = GeneralIdentifier.fromUri(summary.recordDescriptionUri),
          secondaryIdentifier = secondaryIdentifierMap.get(summary.recordDescriptionUri),
          label = summary.scopeAndContent,
          description = summary.scopeAndContent,
          accessRights = accessRightsMap.getOrElse(summary.recordDescriptionUri, List.empty),
          isPartOf = isPartOfMap.getOrElse(summary.recordDescriptionUri, List.empty),
          previousSibling = summary.previousSiblingUri.flatMap(uri => Some(GeneralIdentifier(uri))),
          versionTimestamp = summary.versionTimestamp,
          previousDescription = summary.previousDescriptionUri.flatMap(uri => Some(GeneralIdentifier(uri)))
        ),
        properties = getRecordPropertiesEntity(
          recordPropertiesEntities,
          summary.recordDescriptionUri,
          isReferencedByMap,
          relatedToMap,
          separatedFromMap,
          subjectMap
        )
      )
    )
  }

  private def getAccessRightsMap(accessRightsEntities: List[AccessRightsEntity]): Map[String, List[Identifier]] = {
    val groupedMap: Map[String, List[AccessRightsEntity]] =
      accessRightsEntities.groupBy(_.recordDescriptionUri)
    groupedMap.map(entry => entry._1 -> entry._2.map(a => GeneralIdentifier(a.accessRights)))
  }

  private def getIsPartOfMap(isPartOfEntities: List[IsPartOfEntity]): Map[String, List[Identifier]] =
    isPartOfEntities
      .groupBy(_.recordDescriptionUri.toString)
      .map(entry => entry._1 -> entry._2.map(a => GeneralIdentifier.fromUri(a.recordSetConceptUri)))

  private def getSecondaryIdentifiersMap(
    secondaryIdentifierEntities: List[SecondaryIdentifierEntity]
  ): Map[String, List[TypedIdentifier]] =
    secondaryIdentifierEntities
      .groupBy(_.recordDescriptionUri.toString)
      .map(entry =>
        entry._1 -> entry._2.map(a => TypedIdentifier(GeneralIdentifier(a.secondaryIdentifier), a.identifierProperty))
      )

  private def getLabelledIdentifierMap(
    labelledIdentifierEntities: List[LabelledIdentifierEntity]
  ): Map[String, List[LabelledIdentifier]] =
    labelledIdentifierEntities
      .groupBy(_.recordDescriptionUri.toString)
      .map(entry => entry._1 -> entry._2.map(a => LabelledIdentifier(a.identifier.toString, a.label)))

  private def getSubjectMap(
    uriSubjectEntities: List[IdentifierEntity],
    labelledSubjectEntities: List[LabelledIdentifierEntity]
  ): Map[String, List[Identifier]] = {
    import cats.implicits._
    val map1: Map[String, List[Identifier]] = uriSubjectEntities
      .groupBy(_.recordDescriptionUri.toString)
      .map(entry => entry._1 -> entry._2.map(a => GeneralIdentifier(a.identifier.toString)))
    val map2: Map[String, List[Identifier]] = labelledSubjectEntities
      .groupBy(_.recordDescriptionUri.toString)
      .map(entry => entry._1 -> entry._2.map(a => LabelledIdentifier(a.identifier.toString, a.label)))
    val map3 = map1.combine(map2)
    map3
  }

  private def getRecordPropertiesEntity(
    recordProperties: List[RecordDescriptionPropertiesEntity],
    descriptionUri: String,
    isReferencedByMap: Map[String, List[LabelledIdentifier]],
    relatedToMap: Map[String, List[LabelledIdentifier]],
    separatedFromMap: Map[String, List[LabelledIdentifier]],
    subjectMap: Map[String, List[Identifier]]
  ): RecordDescriptionProperties = {
    val recordPropertiesMap: Map[String, List[RecordDescriptionPropertiesEntity]] =
      recordProperties.groupBy(_.recordDescriptionUri.toString)
    val recordDescriptionProperties: List[RecordDescriptionPropertiesEntity] =
      recordPropertiesMap.getOrElse(descriptionUri, List.empty)
    recordDescriptionProperties.headOption match {
      case Some(props) => propertiesFromEntity(props, isReferencedByMap, relatedToMap, separatedFromMap, subjectMap)
      case None        => RecordDescriptionProperties()
    }
  }

  private def propertiesFromEntity(
    entity: RecordDescriptionPropertiesEntity,
    isReferencedByMap: Map[String, List[LabelledIdentifier]],
    relatedToMap: Map[String, List[LabelledIdentifier]],
    separatedFromMap: Map[String, List[LabelledIdentifier]],
    subjectMap: Map[String, List[Identifier]]
  ): RecordDescriptionProperties =
    RecordDescriptionProperties(
      assetLegalStatus = getLegalStatus(entity),
      legacyTnaCs13RecordType = getCS13RecordType(entity),
      designationOfEdition = entity.designationOfEdition,
      created = getCreated(entity),
      archivistsNote = entity.archivistsNote,
      sourceOfAcquisition = entity.sourceOfAcquisition.flatMap(uri => Some(GeneralIdentifier(uri))),
      custodialHistory = entity.custodialHistory,
      administrativeBiographicalBackground = entity.adminBiogBackground,
      accumulation = getAccumulation(entity),
      appraisal = entity.appraisal,
      accrualPolicy = entity.accrualPolicy.flatMap(uri => Some(GeneralIdentifier(uri))),
      layout = entity.layout,
      publicationNote = entity.publicationNote,
      isReferencedBy = isReferencedByMap.get(entity.recordDescriptionUri),
      relatedTo = relatedToMap.get(entity.recordDescriptionUri),
      separatedFrom = separatedFromMap.get(entity.recordDescriptionUri),
      subject = subjectMap.get(entity.recordDescriptionUri)
    )

  private def getLegalStatus(entity: RecordDescriptionPropertiesEntity): Option[LabelledIdentifier] =
    for {
      legalStatusUri   <- entity.assetLegalStatus
      legalStatusLabel <- entity.legalStatusLabel
    } yield LabelledIdentifier(legalStatusUri, legalStatusLabel)

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
