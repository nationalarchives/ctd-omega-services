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
import uk.gov.nationalarchives.omega.api.repository.model.{ AgentConceptEntity, AgentDescriptionEntity, LegalStatusEntity }

import java.time.ZonedDateTime
import scala.util.Try

class TestRepository extends AbstractRepository {
  override def getLegalStatusEntities: Try[List[LegalStatusEntity]] =
    Try(
      List(
        LegalStatusEntity(new URI(s"${BaseURL.cat}/public-record"), "Public Record"),
        LegalStatusEntity(new URI(s"${BaseURL.cat}/non-public-record"), "Non-Public Record"),
        LegalStatusEntity(
          new URI(s"${BaseURL.cat}/public-record-unless-otherwise-stated"),
          "Public Record (unless otherwise stated)"
        ),
        LegalStatusEntity(new URI(s"${BaseURL.cat}/welsh-public-record"), "Welsh Public Record"),
        LegalStatusEntity(new URI(s"${BaseURL.cat}/non-record-material"), "Non-Record Material")
      )
    )

  override def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): Try[List[AgentConceptEntity]] =
    if (listAgentSummary.depository.getOrElse(false)) {
      Try(
        List(
          AgentConceptEntity(
            new URI(s"${BaseURL.cat}/agent.S7"),
            new URI(s"${BaseURL.cat}/corporate-body-concept"),
            new URI(s"${BaseURL.cat}/agent.S7.1")
          )
        )
      )
    } else {
      Try(
        List(
          AgentConceptEntity(
            new URI(s"${BaseURL.cat}/agent.48N"),
            new URI(s"${BaseURL.cat}/person-concept"),
            new URI(s"${BaseURL.cat}/agent.48N.1")
          ),
          AgentConceptEntity(
            new URI(s"${BaseURL.cat}/agent.46F"),
            new URI(s"${BaseURL.cat}/person-concept"),
            new URI(s"${BaseURL.cat}/agent.46F.1")
          ),
          AgentConceptEntity(
            new URI(s"${BaseURL.cat}/agent.92W"),
            new URI(s"${BaseURL.cat}/corporate-body-concept"),
            new URI(s"${BaseURL.cat}/agent.92W.1")
          ),
          AgentConceptEntity(
            new URI(s"${BaseURL.cat}/agent.8R6"),
            new URI(s"${BaseURL.cat}/corporate-body-concept"),
            new URI(s"${BaseURL.cat}/agent.8R6.1")
          ),
          AgentConceptEntity(
            new URI(s"${BaseURL.cat}/agent.S7"),
            new URI(s"${BaseURL.cat}/corporate-body-concept"),
            new URI(s"${BaseURL.cat}/agent.S7.1")
          )
        )
      )
    }

  override def getAgentDescriptionEntities(
    listAgentSummary: ListAgentSummary,
    agentConceptUri: URI
  ): Try[List[AgentDescriptionEntity]] =
    agentConceptUri.toString match {
      case s"${BaseURL.cat}/agent.48N" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI(s"${BaseURL.cat}/agent.48N.1"),
              "Baden-Powell",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("1889"),
              Some("1977"),
              Some(false),
              None
            )
          )
        )
      case s"${BaseURL.cat}/agent.46F" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI(s"${BaseURL.cat}/agent.46F.1"),
              "Fawkes, Guy",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("1570"),
              Some("1606"),
              Some(false),
              None
            )
          )
        )
      case s"${BaseURL.cat}/agent.92W" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI(s"${BaseURL.cat}/agent.92W.1"),
              "Joint Milk Quality Committee",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("1948"),
              Some("1948"),
              Some(false),
              None
            )
          )
        )
      case s"${BaseURL.cat}/agent.8R6" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI(s"${BaseURL.cat}/agent.8R6.1"),
              "Queen Anne's Bounty",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              None,
              None,
              Some(false),
              None
            )
          )
        )
      case s"${BaseURL.cat}/agent.S7" =>
        Try(
          List(
            AgentDescriptionEntity(
              new URI(s"${BaseURL.cat}/agent.S7.1"),
              "The National Archives, Kew",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("2003"),
              None,
              Some(true),
              None
            )
          )
        )
    }
}
