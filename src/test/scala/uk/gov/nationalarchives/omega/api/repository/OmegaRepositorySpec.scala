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
import org.apache.jena.query.QueryException
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues._
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CorporateBody, Person }
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatus
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.{ AgentDescriptionEntity, AgentSummaryEntity }
import uk.gov.nationalarchives.omega.api.support.UnitTest

import scala.util.{ Failure, Success, Try }

class OmegaRepositorySpec extends UnitTest {

  private val mockConnector = mock[SparqlEndpointConnector]
  private val repository = new OmegaRepository(mockConnector)

  "Get Legal Status summaries" - {

    "must return a Success with an empty list" in {
      when(mockConnector.execute[LegalStatus](any, any)).thenReturn(Success(List.empty))
      val result = repository.getLegalStatusEntities
      result.success.get.length mustBe 0
    }
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LegalStatus](any, any)).thenReturn(
        Success(List(LegalStatus(new URI("http://cat.nationalarchives.gov.uk/public-record"), "Public Record")))
      )
      val result = repository.getLegalStatusEntities
      result.success.get.length mustBe 1
    }
    "must return a Failure with an exception" in {
      val errorMessage = "There was a problem"
      when(mockConnector.execute[LegalStatus](any, any)).thenReturn(Failure(new QueryException(errorMessage)))
      val result = repository.getLegalStatusEntities
      result.failure.exception.getMessage must equal(errorMessage)
    }
  }

  "Get Agent Summary Entities" - {

    "must return a Success with a list of one item" in {
      when(mockConnector.execute[AgentSummaryEntity](any, any)).thenReturn(
        Try(
          List(
            AgentSummaryEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG"),
              new URI("http://cat.nationalarchives.gov.uk/person-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1")
            )
          )
        )
      )
      val result = repository.getAgentSummaryEntities(ListAgentSummary(Some(List(Person))))
      result.success.get.length mustBe 1
    }

    "must return a Success with a list of two items" in {
      when(mockConnector.execute[AgentSummaryEntity](any, any)).thenReturn(
        Try(
          List(
            AgentSummaryEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG"),
              new URI("http://cat.nationalarchives.gov.uk/person-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1")
            ),
            AgentSummaryEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.S7"),
              new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.S7.1")
            )
          )
        )
      )
      val result = repository.getAgentSummaryEntities(ListAgentSummary(Some(List(CorporateBody, Person))))
      result.success.get.length mustBe 2
    }
    "must return a Success with a place of deposit" in {
      when(mockConnector.execute[AgentSummaryEntity](any, any)).thenReturn(
        Try(
          List(
            AgentSummaryEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.S7"),
              new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.S7.1")
            )
          )
        )
      )
      val result =
        repository.getAgentSummaryEntities(ListAgentSummary(Some(List(CorporateBody)), depository = Some(true)))
      result.success.get.length mustBe 1
    }
  }
  "must return a Success with a list of one item" in {
    when(mockConnector.execute[AgentSummaryEntity](any, any)).thenReturn(
      Try(
        List(
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.3LG"),
            new URI("http://cat.nationalarchives.gov.uk/person-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1")
          )
        )
      )
    )
    val result = repository.getAgentSummaryEntities(ListAgentSummary(Some(List(Person))))
    result.success.get.length mustBe 1
  }
  "Get Agent Description Entities" - {
    "must return one agent description" in {
      when(mockConnector.execute[AgentDescriptionEntity](any, any)).thenReturn(
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1"),
              "Edwin Hill",
              "2023-01-25T14:18:41.668Z",
              Some("1793"),
              Some("1876"),
              depository = Some(false),
              None
            )
          )
        )
      )
      val result = repository.getAgentDescriptionEntities(
        ListAgentSummary(),
        new URI("http://cat.nationalarchives.gov.uk/agent.3LG")
      )
      result.success.get.length mustBe 1
    }
    "must return two agent descriptions" in {
      when(mockConnector.execute[AgentDescriptionEntity](any, any)).thenReturn(
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.2"),
              "Edmond Hill",
              "2023-07-27T12:45:00.000Z",
              Some("1793"),
              Some("1876"),
              depository = Some(false),
              None
            ),
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1"),
              "Edwin Hill",
              "2023-01-25T14:18:41.668Z",
              Some("1793"),
              Some("1876"),
              depository = Some(false),
              None
            )
          )
        )
      )
      val result = repository.getAgentDescriptionEntities(
        ListAgentSummary(),
        new URI("http://cat.nationalarchives.gov.uk/agent.3LG")
      )
      result.success.get.length mustBe 2
    }
  }
}
