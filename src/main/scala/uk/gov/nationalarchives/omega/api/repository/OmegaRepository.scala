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

package uk.gov.nationalarchives.omega.api.repository

import cats.effect.IO
import org.apache.jena.ext.xerces.util.URI
import org.apache.jena.query.{ Query, QuerySolution }
import org.phenoscape.sparql.FromQuerySolution
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model._
import uk.gov.nationalarchives.omega.api.repository.vocabulary.Cat

import java.time.ZonedDateTime
import scala.util.Try

class OmegaRepository(sparqlConnector: SparqlEndpointConnector) extends AbstractRepository with RepositoryUtils {

  private val sparqlResourceDir = "sparql"
  private val selectLegalStatusSummarySparqlResource = s"/$sparqlResourceDir/select-legal-status-summaries.rq"
  private val getAgentSummariesSparqlResource = s"/$sparqlResourceDir/get-agent-concepts.rq"
  private val getAgentDescriptionsSparqlResource = s"/$sparqlResourceDir/get-agent-descriptions.rq"
  private val getRecordConceptSparqlResource = s"/$sparqlResourceDir/get-record-concept.rq"
  private val getRecordCreatorSparqlResource = s"/$sparqlResourceDir/get-record-creator.rq"
  private val getRecordDescriptionSummarySparqlResource = s"/$sparqlResourceDir/get-record-description-summary.rq"
  private val getRecordDescriptionPropertiesSparqlResource = s"/$sparqlResourceDir/get-record-description-properties.rq"
  private val getAccessRightsSparqlResource = s"/$sparqlResourceDir/get-access-rights.rq"
  private val getIsPartOfSparqlResource = s"/$sparqlResourceDir/get-is-part-of.rq"
  private val getSecondaryIdentifiersSparqlResource = s"/$sparqlResourceDir/get-secondary-identifiers.rq"
  private val getIsReferencedBySparqlResource = s"/$sparqlResourceDir/get-is-referenced-by.rq"
  private val getDctRelationByTypeSparqlResource = s"/$sparqlResourceDir/get-dct-relation-by-type.rq"
  private val getSubjectUriSparqlResource = s"/$sparqlResourceDir/get-subject-uri.rq"
  private val getLabelledSubjectUriSparqlResource = s"/$sparqlResourceDir/get-labelled-subject-uri.rq"

  implicit object BooleanFromQuerySolution extends FromQuerySolution[Boolean] {
    def fromQuerySolution(qs: QuerySolution, variablePath: String = ""): Try[Boolean] =
      getLiteral(qs, variablePath).map(_.getBoolean)
  }

  implicit object ZoneDateTimeFromQuerySolution extends FromQuerySolution[ZonedDateTime] {
    def fromQuerySolution(qs: QuerySolution, variablePath: String = ""): Try[ZonedDateTime] =
      getLiteral(qs, variablePath).map(value => ZonedDateTime.parse(value.getString))
  }

  override def getLegalStatusEntities: IO[List[LegalStatusEntity]] =
    for {
      query  <- prepareQuery(selectLegalStatusSummarySparqlResource)
      result <- executeQuery(query, implicitly[FromQuerySolution[LegalStatusEntity]])
    } yield result

  override def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): IO[List[AgentConceptEntity]] =
    for {
      params <- SparqlParams.from(listAgentSummary)
      query  <- prepareParameterizedQuery(getAgentSummariesSparqlResource, params)
      result <- executeQuery(query, implicitly[FromQuerySolution[AgentConceptEntity]])
    } yield result

  override def getAgentDescriptionEntities(
    listAgentSummary: ListAgentSummary,
    agentConceptUri: URI
  ): IO[List[AgentDescriptionEntity]] =
    for {
      params <- SparqlParams.from(listAgentSummary)
      query <- prepareParameterizedQuery(
                 getAgentDescriptionsSparqlResource,
                 params.copy(uris = params.uris ++ Map("conceptIdParam" -> agentConceptUri.toString)),
                 extendQuery = true
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[AgentDescriptionEntity]])
    } yield result

  override def getRecordConceptEntity(recordConceptId: String): IO[List[RecordConceptEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getRecordConceptSparqlResource,
                 SparqlParams(strings = Map("recordConceptId" -> recordConceptId))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[RecordConceptEntity]])
    } yield result

  override def getCreatorEntities(recordConceptUri: String): IO[List[CreatorEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getRecordCreatorSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[CreatorEntity]])
    } yield result

  override def getRecordDescriptionSummaries(recordConceptUri: String): IO[List[RecordDescriptionSummaryEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getRecordDescriptionSummarySparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[RecordDescriptionSummaryEntity]])
    } yield result

  override def getRecordDescriptionProperties(recordConceptUri: String): IO[List[RecordDescriptionPropertiesEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getRecordDescriptionPropertiesSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[RecordDescriptionPropertiesEntity]])
    } yield result

  override def getAccessRights(recordConceptUri: String): IO[List[AccessRightsEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getAccessRightsSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[AccessRightsEntity]])
    } yield result

  override def getIsPartOf(recordConceptUri: String): IO[List[IsPartOfEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getIsPartOfSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[IsPartOfEntity]])
    } yield result

  override def getSecondaryIdentifiers(recordConceptUri: String): IO[List[SecondaryIdentifierEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getSecondaryIdentifiersSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[SecondaryIdentifierEntity]])
    } yield result

  override def getIsReferencedBys(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getIsReferencedBySparqlResource,
                 SparqlParams(uris =
                   Map(
                     "recordConceptUri" -> recordConceptUri
                   )
                 )
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[LabelledIdentifierEntity]])
    } yield result

  override def getRelatedTos(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getDctRelationByTypeSparqlResource,
                 SparqlParams(uris =
                   Map(
                     "recordConceptUri" -> recordConceptUri,
                     "relationType"     -> Cat.relatedMaterial
                   )
                 )
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[LabelledIdentifierEntity]])
    } yield result

  override def getSeparatedFroms(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getDctRelationByTypeSparqlResource,
                 SparqlParams(uris =
                   Map(
                     "recordConceptUri" -> recordConceptUri,
                     "relationType"     -> Cat.separatedMaterial
                   )
                 )
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[LabelledIdentifierEntity]])
    } yield result

  override def getUriSubjects(recordConceptUri: String): IO[List[IdentifierEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getSubjectUriSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[IdentifierEntity]])
    } yield result

  override def getLabelledSubjects(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    for {
      query <- prepareParameterizedQuery(
                 getLabelledSubjectUriSparqlResource,
                 SparqlParams(uris = Map("recordConceptUri" -> recordConceptUri))
               )
      result <- executeQuery(query, implicitly[FromQuerySolution[LabelledIdentifierEntity]])
    } yield result

  private def executeQuery[A](query: Query, queryDecoder: FromQuerySolution[A]): IO[List[A]] =
    sparqlConnector.execute(query, queryDecoder)

}
