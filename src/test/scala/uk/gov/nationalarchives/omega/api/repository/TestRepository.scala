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
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model._
import uk.gov.nationalarchives.omega.api.repository.vocabulary.{ Cat, TNA, Time }

import java.time.ZonedDateTime

class TestRepository extends AbstractRepository {
  override def getLegalStatusEntities: IO[List[LegalStatusEntity]] =
    IO(
      List(
        LegalStatusEntity(new URI(s"${Cat.NS}public-record"), "Public Record"),
        LegalStatusEntity(new URI(s"${Cat.NS}non-public-record"), "Non-Public Record"),
        LegalStatusEntity(
          new URI(s"${Cat.NS}public-record-unless-otherwise-stated"),
          "Public Record (unless otherwise stated)"
        ),
        LegalStatusEntity(new URI(s"${Cat.NS}welsh-public-record"), "Welsh Public Record"),
        LegalStatusEntity(new URI(s"${Cat.NS}non-record-material"), "Non-Record Material")
      )
    )

  override def getAgentSummaryEntities(listAgentSummary: ListAgentSummary): IO[List[AgentConceptEntity]] =
    if (listAgentSummary.depository.getOrElse(false)) {
      IO(
        List(
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.S7"),
            new URI(s"${Cat.NS}corporate-body-concept"),
            new URI(s"${Cat.NS}agent.S7.1")
          )
        )
      )
    } else {
      IO(
        List(
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.48N"),
            new URI(s"${Cat.NS}person-concept"),
            new URI(s"${Cat.NS}agent.48N.1")
          ),
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.46F"),
            new URI(s"${Cat.NS}person-concept"),
            new URI(s"${Cat.NS}agent.46F.1")
          ),
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.92W"),
            new URI(s"${Cat.NS}corporate-body-concept"),
            new URI(s"${Cat.NS}agent.92W.1")
          ),
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.8R6"),
            new URI(s"${Cat.NS}corporate-body-concept"),
            new URI(s"${Cat.NS}agent.8R6.1")
          ),
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.S7"),
            new URI(s"${Cat.NS}corporate-body-concept"),
            new URI(s"${Cat.NS}agent.S7.1")
          )
        )
      )
    }

  override def getAgentDescriptionEntities(
    listAgentSummary: ListAgentSummary,
    agentConceptUri: URI
  ): IO[List[AgentDescriptionEntity]] =
    agentConceptUri.toString match {
      case s"${Cat.NS}agent.48N" =>
        IO(
          List(
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.48N.1"),
              "Baden-Powell",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("1889"),
              Some("1977"),
              Some(false),
              None
            )
          )
        )
      case s"${Cat.NS}agent.46F" =>
        IO(
          List(
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.46F.1"),
              "Fawkes, Guy",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("1570"),
              Some("1606"),
              Some(false),
              None
            )
          )
        )
      case s"${Cat.NS}agent.92W" =>
        IO(
          List(
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.92W.1"),
              "Joint Milk Quality Committee",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              Some("1948"),
              Some("1948"),
              Some(false),
              None
            )
          )
        )
      case s"${Cat.NS}agent.8R6" =>
        IO(
          List(
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.8R6.1"),
              "Queen Anne's Bounty",
              ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
              None,
              None,
              Some(false),
              None
            )
          )
        )
      case s"${Cat.NS}agent.S7" =>
        IO(
          List(
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.S7.1"),
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

  override def getRecordConceptEntity(recordConceptId: String): IO[List[RecordConceptEntity]] =
    IO(
      List(
        RecordConceptEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P"),
          new URI(TNA.ontPhysicalRecord),
          new URI(s"${Cat.NS}COAL.2022.N373.P.1")
        )
      )
    )

  override def getCreatorEntities(agentConceptId: String): IO[List[CreatorEntity]] =
    IO(
      List(
        CreatorEntity(new URI(s"${Cat.NS}agent.24"), "from 1965")
      )
    )

  override def getRecordDescriptionSummaries(recordConceptUri: String): IO[List[RecordDescriptionSummaryEntity]] =
    IO(
      List(
        RecordDescriptionSummaryEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Deryk Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
          "2023-08-30T12:10:00.000Z",
          Some(new URI(s"${Cat.NS}COAL.2022.N3HQ.P.1")),
          Some(new URI(s"${Cat.NS}COAL.2022.N373.P.1"))
        ),
        RecordDescriptionSummaryEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Derick Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
          "2023-08-30T12:10:00.000Z",
          Some(new URI(s"${Cat.NS}COAL.2022.N3HQ.P.1")),
          Some(new URI(s"${Cat.NS}COAL.2022.N373.P.1"))
        )
      )
    )

  override def getRecordDescriptionProperties(recordConceptUri: String): IO[List[RecordDescriptionPropertiesEntity]] =
    IO(
      List(
        RecordDescriptionPropertiesEntity(
          recordDescriptionUri = new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          assetLegalStatus = Some(new URI(s"${Cat.NS}public-record")),
          legalStatusLabel = Some("Public Record"),
          legacyType = Some(new URI(s"${Cat.NS}item")),
          designationOfEdition = Some("<unittitle type=\"Map Designation\">GSGS 2321</unittitle>"),
          createdType = Some(new URI(Time.ProperInterval)),
          createdDescription = Some("1963"),
          createdBeginning = Some("1963-01-01Z"),
          createdEnd = Some("1963-12-31Z"),
          archivistsNote = Some("[Grid reference: N/A]"),
          sourceOfAcquisition = Some(new URI(s"${Cat.NS}agent.24")),
          custodialHistory = Some("Retained until 2006"),
          adminBiogBackground =
            Some("<bioghist><p>The board met periodically until 1935 when it was allowed to lapse.</p></bioghist>"),
          accumulationType = Some(new URI(Time.ProperInterval)),
          accumulationDescription = Some("1963"),
          accumulationBeginning = Some("1963-01-01Z"),
          accumulationEnd = Some("1963-12-31Z"),
          appraisal = Some("Files selected in accordance with Operational Selection Policy OSP 25"),
          accrualPolicy = Some(new URI(s"${Cat.NS}policy.Series_is_accruing")),
          layout = Some("Photographs in an envelope"),
          publicationNote = Some("Some of the photographs in this series appeared in The Times newspaper.")
        ),
        RecordDescriptionPropertiesEntity(
          recordDescriptionUri = new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          assetLegalStatus = Some(new URI(s"${Cat.NS}public-record")),
          legalStatusLabel = Some("Public Record"),
          legacyType = Some(new URI(s"${Cat.NS}item")),
          createdType = Some(new URI(Time.Instant)),
          createdDescription = Some("1963"),
          createdInstant = Some("1963-01-01Z"),
          archivistsNote = Some("[Grid reference: NX 509 582]"),
          sourceOfAcquisition = Some(new URI(s"${Cat.NS}agent.25")),
          custodialHistory = Some("Retained until 2001"),
          adminBiogBackground =
            Some("<bioghist><p>The board met periodically until 1936 when it was allowed to lapse.</p></bioghist>"),
          accumulationType = Some(new URI(Time.Instant)),
          accumulationDescription = Some("1963"),
          accumulationInstant = Some("1963-01-01Z"),
          appraisal = Some("Files selected in accordance with Operational Selection Policy OSP 26"),
          accrualPolicy = Some(new URI(s"${Cat.NS}policy.No_future_accruals_expected")),
          layout = Some("Photographs in a box"),
          publicationNote =
            Some("Some of the photographs in this series appeared in The Manchester Guardian newspaper.")
        )
      )
    )

  override def getAccessRights(recordConceptUri: String): IO[List[AccessRightsEntity]] =
    IO(
      List(
        AccessRightsEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          new URI(s"${Cat.NS}policy.Open_Description")
        ),
        AccessRightsEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          new URI(s"${Cat.NS}policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31")
        ),
        AccessRightsEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          new URI(s"${Cat.NS}policy.Open_Description")
        ),
        AccessRightsEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          new URI(s"${Cat.NS}policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31")
        )
      )
    )

  override def getIsPartOf(recordConceptUri: String): IO[List[IsPartOfEntity]] =
    IO(
      List(
        IsPartOfEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          new URI(s"${Cat.NS}recordset.COAL.2022.2834")
        ),
        IsPartOfEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          new URI(s"${Cat.NS}recordset.COAL.2022.2834")
        )
      )
    )

  override def getSecondaryIdentifiers(recordConceptUri: String): IO[List[SecondaryIdentifierEntity]] =
    IO(
      List(
        SecondaryIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          new URI(s"${Cat.NS}classicCatalogueReference"),
          "COAL 80/2052/9"
        ),
        SecondaryIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          new URI(s"${Cat.NS}classicCatalogueReference"),
          "COAL 80/2052/9"
        )
      )
    )

  override def getIsReferencedBys(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    IO(
      List(
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          "Coal Board Minutes 1963",
          new URI(s"${Cat.NS}res.JN31")
        ),
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          "Coal Board Minutes 1962",
          new URI(s"${Cat.NS}res.4JJF")
        )
      )
    )

  override def getRelatedTos(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    IO(
      List(
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          "Index of colliery photographs March 1963",
          new URI(s"${Cat.NS}COAL.2022.S144")
        ),
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          "Index of colliery photographs September 1963",
          new URI(s"${Cat.NS}COAL.2022.G221")
        )
      )
    )

  override def getSeparatedFroms(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    IO(
      List(
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          "NCB records 1963",
          new URI(s"${Cat.NS}CAB.2022.L744")
        ),
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
          "Cabinet records 1963",
          new URI(s"${Cat.NS}CAB.2022.N901")
        )
      )
    )

  override def getUriSubjects(recordConceptUri: String): IO[List[IdentifierEntity]] =
    IO(
      List(
        IdentifierEntity(new URI(s"${Cat.NS}COAL.2022.N373.P.2"), new URI(s"${Cat.NS}agent.4N6")),
        IdentifierEntity(new URI(s"${Cat.NS}COAL.2022.N373.P.2"), new URI(s"${Cat.NS}agent.S7"))
      )
    )

  override def getLabelledSubjects(recordConceptUri: String): IO[List[LabelledIdentifierEntity]] =
    IO(
      List(
        LabelledIdentifierEntity(
          new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
          "from 1965",
          new URI(s"${Cat.NS}agent.24")
        )
      )
    )
}
