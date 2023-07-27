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
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatus
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.{ AgentDescriptionEntity, AgentSummaryEntity }

import scala.util.Try

class TestRepository extends AbstractRepository {
  override def getLegalStatusSummaries: Try[List[LegalStatus]] = Try(List())

  // override def getAgentEntities: Try[List[AgentEntity]] = Try(List())

  // override def getPlaceOfDepositEntities: Try[List[AgentEntity]] = Try(List())

  override def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): Try[List[AgentSummaryEntity]] =
    if (listAgentSummary.depository.getOrElse(false)) {
      Try(
        List(
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.S7"),
            new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.S7.1")
          )
        )
      )
    } else {
      Try(
        List(
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.48N"),
            new URI("http://cat.nationalarchives.gov.uk/person-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.48N.1")
          ),
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.46F"),
            new URI("http://cat.nationalarchives.gov.uk/person-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.46F.1")
          ),
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.92W"),
            new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.92W.1")
          ),
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.8R6"),
            new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.8R6.1")
          ),
          AgentSummaryEntity(
            new URI("http://cat.nationalarchives.gov.uk/agent.S7"),
            new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
            new URI("http://cat.nationalarchives.gov.uk/agent.S7.1")
          )
        )
      )
    }

  override def getAgentDescriptionEntities(
    listAgentSummary: ListAgentSummary,
    agentConceptUri: URI
  ): Try[List[AgentDescriptionEntity]] =
    agentConceptUri.toString match {
      case "http://cat.nationalarchives.gov.uk/agent.48N" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.48N.1"),
              "Baden-Powell",
              "2022-06-22T02:00:00-0500",
              Some("1889"),
              Some("1977"),
              Some(false),
              None
            )
          )
        )
      case "http://cat.nationalarchives.gov.uk/agent.46F" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.46F.1"),
              "Fawkes, Guy",
              "2022-06-22T02:00:00-0500",
              Some("1570"),
              Some("1606"),
              Some(false),
              None
            )
          )
        )
      case "http://cat.nationalarchives.gov.uk/agent.92W" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.92W.1"),
              "Joint Milk Quality Committee",
              "2022-06-22T02:00:00-0500",
              Some("1948"),
              Some("1948"),
              Some(false),
              None
            )
          )
        )
      case "http://cat.nationalarchives.gov.uk/agent.8R6" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.8R6.1"),
              "Queen Anne's Bounty",
              "2022-06-22T02:00:00-0500",
              None,
              None,
              Some(false),
              None
            )
          )
        )
      case "http://cat.nationalarchives.gov.uk/agent.S7" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI("http://cat.nationalarchives.gov.uk/agent.S7.1"),
              "The National Archives, Kew",
              "2022-06-22T02:00:00-0500",
              Some("2003"),
              None,
              Some(true),
              None
            )
          )
        )
    }
}
