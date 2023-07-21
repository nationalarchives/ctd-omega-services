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
import org.mockito.MockitoSugar
import org.scalatest.TryValues._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.AgentType
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CorporateBody, Person }
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatus
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.AgentEntity

import scala.util.{ Failure, Success, Try }

class OmegaRepositorySpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val mockConnector = mock[SparqlEndpointConnector]
  private val repository = new OmegaRepository(mockConnector)

  "Get Legal Status summaries" - {

    "must return a Success with an empty list" in {
      when(mockConnector.execute[LegalStatus](any, any)).thenReturn(Success(List.empty))
      val result = repository.getLegalStatusSummaries
      result.success.get.length mustBe 0
    }
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LegalStatus](any, any)).thenReturn(
        Success(List(LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/public-record"), "Public Record")))
      )
      val result = repository.getLegalStatusSummaries
      result.success.get.length mustBe 1
    }
    "must return a Failure with an exception" in {
      val errorMessage = "There was a problem"
      when(mockConnector.execute[LegalStatus](any, any)).thenReturn(Failure(new QueryException(errorMessage)))
      val result = repository.getLegalStatusSummaries
      result.failure.exception.getMessage must equal(errorMessage)
    }
  }

  "Get List Agent Summaries" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[AgentEntity](any, any)).thenReturn(
        Try(
          List(
            AgentEntity(
              new URI("http://cat.nationalarchives.gov.uk/person-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG"),
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1"),
              "Edwin Hill",
              "2023-01-25T14:18:41.668Z",
              Some("1876"),
              Some("1793"),
              Some(false)
            )
          )
        )
      )
      val result = repository.getAgentEntities
      result.success.get.length mustBe 1
    }

    "must return a Success with a place of deposit" in {
      when(mockConnector.execute[AgentEntity](any, any)).thenReturn(
        Try(
          List(
            AgentEntity(
              new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.S7"),
              new URI("http://cat.nationalarchives.gov.uk/agent.S7"),
              "The National Archives, Kew",
              "2022-06-22T02:00:00-0500",
              Some("2003"),
              None,
              Some(true)
            )
          )
        )
      )
      val result = repository.getPlaceOfDepositEntities
      result.success.get.length mustBe 1
    }
  }

  "Get Agent Summary Entities" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[AgentEntity](any, any)).thenReturn(
        Try(
          List(
            AgentEntity(
              new URI("http://cat.nationalarchives.gov.uk/person-concept"),
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG"),
              new URI("http://cat.nationalarchives.gov.uk/agent.3LG.1"),
              "Edwin Hill",
              "2023-01-25T14:18:41.668Z",
              Some("1876"),
              Some("1793"),
              Some(false)
            )
          )
        )
      )
      val result = repository.getAgentSummaryEntities(ListAgentSummary(List(CorporateBody), depository = Some(true)))
      result.success.get.length mustBe 1

    }
  }
}
