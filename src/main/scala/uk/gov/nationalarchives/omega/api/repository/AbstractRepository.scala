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

import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model._

import scala.util.Try

/** The AbstractRepository defines the method signatures require for interacting with the data repository and is
  * agnostic as to the type of repository being used. Specific implementations will be required for different repository
  * types. For example, the production repository may be a triplestore but a test implementation may just contain some
  * hard-coded implementation or read data from the file system.
  */
trait AbstractRepository {

  /** Retrieve all of the legal statuses from the repository
    * @return
    *   a Success with a list of LegalStatusEntity objects or an error
    */
  def getLegalStatusEntities: Try[List[LegalStatusEntity]]

  /** Retrieve the agent concepts from the repository based on the given request
    * @param listAgentSummary
    *   the agent summary request
    * @return
    *   a Success with a list of AgentConceptEntity objects or an error
    */
  def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): Try[List[AgentConceptEntity]]

  /** Retrieve the agent descriptions for the given concept URI based on the given request
    * @param listAgentSummary
    *   the agent summary request
    * @param agentConceptUri
    *   the agent concept URI
    * @return
    *   a Success with a list of AgentDescriptionEntity objects or an error
    */
  def getAgentDescriptionEntities(
    listAgentSummary: ListAgentSummary,
    agentConceptUri: URI
  ): Try[List[AgentDescriptionEntity]]

  def getRecordConceptEntity(recordConceptId: String): Try[List[RecordConceptEntity]]

  def getCreatorEntities(recordConceptUri: String): Try[List[CreatorEntity]]

  def getRecordDescriptionSummaries(recordConceptUri: String): Try[List[RecordDescriptionSummaryEntity]]

  def getRecordDescriptionProperties(recordConceptUri: String): Try[List[RecordDescriptionPropertiesEntity]]

  def getAccessRights(recordConceptUri: String): Try[List[AccessRightsEntity]]

  def getIsPartOf(recordConceptUri: String): Try[List[IsPartOfEntity]]

  def getSecondaryIdentifiers(recordConceptUri: String): Try[List[SecondaryIdentifierEntity]]

  def getIsReferencedBy(recordConceptUri: String): Try[List[LabelledIdentifierEntity]]

}
