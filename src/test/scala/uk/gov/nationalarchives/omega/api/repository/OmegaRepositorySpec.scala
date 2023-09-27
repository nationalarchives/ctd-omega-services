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
import uk.gov.nationalarchives.omega.api.messages.reply.LegalStatusSummary
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.model.{ AccessRightsEntity, AgentConceptEntity, AgentDescriptionEntity, CreatorEntity, IdentifierEntity, IsPartOfEntity, LabelledIdentifierEntity, RecordConceptEntity, RecordDescriptionPropertiesEntity, RecordDescriptionSummaryEntity, SecondaryIdentifierEntity }
import uk.gov.nationalarchives.omega.api.repository.vocabulary.{ Cat, Time }
import uk.gov.nationalarchives.omega.api.support.UnitTest

import java.time.ZonedDateTime
import scala.util.{ Failure, Success, Try }

class OmegaRepositorySpec extends UnitTest {

  private val mockConnector = mock[SparqlEndpointConnector]
  private val repository = new OmegaRepository(mockConnector)

  "Get Legal Status summaries" - {

    "must return a Success with an empty list" in {
      when(mockConnector.execute[LegalStatusSummary](any, any)).thenReturn(Success(List.empty))
      val result = repository.getLegalStatusEntities
      result.success.get.length mustBe 0
    }
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LegalStatusSummary](any, any)).thenReturn(
        Success(List(LegalStatusSummary(new URI(s"${Cat.NS}public-record"), "Public Record")))
      )
      val result = repository.getLegalStatusEntities
      result.success.get.length mustBe 1
    }
    "must return a Failure with an exception" in {
      val errorMessage = "There was a problem"
      when(mockConnector.execute[LegalStatusSummary](any, any)).thenReturn(Failure(new QueryException(errorMessage)))
      val result = repository.getLegalStatusEntities
      result.failure.exception.getMessage must equal(errorMessage)
    }
  }

  "Get Agent Summary Entities" - {

    "must return a Success with a list of one item" in {
      when(mockConnector.execute[AgentConceptEntity](any, any)).thenReturn(
        Try(
          List(
            AgentConceptEntity(
              new URI(s"${Cat.NS}agent.3LG"),
              new URI(s"${Cat.NS}person-concept"),
              new URI(s"${Cat.NS}agent.3LG.1")
            )
          )
        )
      )
      val result = repository.getAgentSummaryEntities(ListAgentSummary(Some(List(Person))))
      result.success.get.length mustBe 1
    }

    "must return a Success with a list of two items" in {
      when(mockConnector.execute[AgentConceptEntity](any, any)).thenReturn(
        Try(
          List(
            AgentConceptEntity(
              new URI(s"${Cat.NS}agent.3LG"),
              new URI(s"${Cat.NS}person-concept"),
              new URI(s"${Cat.NS}agent.3LG.1")
            ),
            AgentConceptEntity(
              new URI(s"${Cat.NS}agent.S7"),
              new URI(s"${Cat.NS}corporate-body-concept"),
              new URI(s"${Cat.NS}agent.S7.1")
            )
          )
        )
      )
      val result = repository.getAgentSummaryEntities(ListAgentSummary(Some(List(CorporateBody, Person))))
      result.success.get.length mustBe 2
    }
    "must return a Success with a place of deposit" in {
      when(mockConnector.execute[AgentConceptEntity](any, any)).thenReturn(
        Try(
          List(
            AgentConceptEntity(
              new URI(s"${Cat.NS}agent.S7"),
              new URI(s"${Cat.NS}corporate-body-concept"),
              new URI(s"${Cat.NS}agent.S7.1")
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
    when(mockConnector.execute[AgentConceptEntity](any, any)).thenReturn(
      Try(
        List(
          AgentConceptEntity(
            new URI(s"${Cat.NS}agent.3LG"),
            new URI(s"${Cat.NS}person-concept"),
            new URI(s"${Cat.NS}agent.3LG.1")
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
              new URI(s"${Cat.NS}agent.3LG.1"),
              "Edwin Hill",
              ZonedDateTime.parse("2023-01-25T14:18:41.668Z"),
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
        new URI(s"${Cat.NS}agent.3LG")
      )
      result.success.get.length mustBe 1
    }
    "must return two agent descriptions" in {
      when(mockConnector.execute[AgentDescriptionEntity](any, any)).thenReturn(
        Try(
          List(
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.3LG.2"),
              "Edmond Hill",
              ZonedDateTime.parse("2023-07-27T12:45:00.000Z"),
              Some("1793"),
              Some("1876"),
              depository = Some(false),
              None
            ),
            AgentDescriptionEntity(
              new URI(s"${Cat.NS}agent.3LG.1"),
              "Edwin Hill",
              ZonedDateTime.parse("2023-01-25T14:18:41.668Z"),
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
        new URI(s"${Cat.NS}agent.3LG")
      )
      result.success.get.length mustBe 2
    }
  }

  "Get Record Concept Entities" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[RecordConceptEntity](any, any)).thenReturn(
        Try(
          List(
            RecordConceptEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P"),
              new URI("http://www.nationalarchives.gov.uk/ont.physical-record"),
              new URI(s"${Cat.NS}COAL.2022.N373.P.1")
            )
          )
        )
      )
      val result = repository.getRecordConceptEntity(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1

    }
    "must return a Success with an empty list if no matching record concept is found" in {
      when(mockConnector.execute[RecordConceptEntity](any, any)).thenReturn(Try(List.empty))
      val result = repository.getRecordConceptEntity(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 0
    }
  }

  "Get Creator Entities" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[CreatorEntity](any, any)).thenReturn(
        Try(
          List(
            CreatorEntity(new URI(s"${Cat.NS}agent.24"), "from 1965")
          )
        )
      )
      val result = repository.getCreatorEntities(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[CreatorEntity](any, any)).thenReturn(
        Try(
          List(
            CreatorEntity(new URI(s"${Cat.NS}agent.24"), "from 1965"),
            CreatorEntity(new URI(s"${Cat.NS}agent.S7"), "from 1968")
          )
        )
      )
      val result = repository.getCreatorEntities(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Record Description Summaries" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[RecordDescriptionSummaryEntity](any, any)).thenReturn(
        Try(
          List(
            RecordDescriptionSummaryEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "<scopecontent><p>Coal News albums 1963</p></scopecontent>",
              "2023-08-30T12:10:00.000Z",
              Some(new URI(s"${Cat.NS}COAL.2022.N3HQ.P.1")),
              Some(new URI(s"${Cat.NS}COAL.2022.N373.P.1"))
            )
          )
        )
      )
      val result = repository.getRecordDescriptionSummaries(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[RecordDescriptionSummaryEntity](any, any)).thenReturn(
        Try(
          List(
            RecordDescriptionSummaryEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "<scopecontent><p>Coal News albums 1963</scopecontent>",
              "2023-08-30T12:10:00.000Z",
              Some(new URI(s"${Cat.NS}COAL.2022.N3HQ.P.1")),
              Some(new URI(s"${Cat.NS}COAL.2022.N373.P.1"))
            ),
            RecordDescriptionSummaryEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.1"),
              "<scopecontent><p>Coal News albums 1964</p></scopecontent>",
              "2023-08-30T12:10:00.000Z",
              Some(new URI(s"${Cat.NS}COAL.2022.N3HQ.P.1")),
              Some(new URI(s"${Cat.NS}COAL.2022.N373.P.1"))
            )
          )
        )
      )
      val result = repository.getRecordDescriptionSummaries(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Record Description Properties" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[RecordDescriptionPropertiesEntity](any, any)).thenReturn(
        Try(
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
            )
          )
        )
      )
      val result = repository.getRecordDescriptionProperties(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[RecordDescriptionPropertiesEntity](any, any)).thenReturn(
        Try(
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
      )
      val result = repository.getRecordDescriptionProperties(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Access Rights" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[AccessRightsEntity](any, any)).thenReturn(
        Try(
          List(
            AccessRightsEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              new URI(s"${Cat.NS}policy.Open_Description")
            )
          )
        )
      )
      val result = repository.getAccessRights(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[AccessRightsEntity](any, any)).thenReturn(
        Try(
          List(
            AccessRightsEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              new URI(s"${Cat.NS}policy.Open_Description")
            ),
            AccessRightsEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              new URI(s"${Cat.NS}policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31")
            )
          )
        )
      )
      val result = repository.getAccessRights(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Is Part Of" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[IsPartOfEntity](any, any)).thenReturn(
        Try(
          List(
            IsPartOfEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              new URI(s"${Cat.NS}recordset.COAL.2022.2834")
            )
          )
        )
      )
      val result = repository.getIsPartOf(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[IsPartOfEntity](any, any)).thenReturn(
        Try(
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
      )
      val result = repository.getIsPartOf(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Secondary Identifiers" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[SecondaryIdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            SecondaryIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              new URI(s"${Cat.NS}classicCatalogueReference"),
              "COAL 80/2052/9"
            )
          )
        )
      )
      val result = repository.getSecondaryIdentifiers(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[SecondaryIdentifierEntity](any, any)).thenReturn(
        Try(
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
      )
      val result = repository.getSecondaryIdentifiers(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Is Referenced By" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            LabelledIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "Coal Board Minutes 1963",
              new URI(s"${Cat.NS}res.JN31")
            )
          )
        )
      )
      val result = repository.getIsReferencedBys(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
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
      )
      val result = repository.getIsReferencedBys(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Related To" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            LabelledIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "Index of colliery photographs March 1963",
              new URI(s"${Cat.NS}COAL.2022.S144")
            )
          )
        )
      )
      val result = repository.getRelatedTos(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
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
      )
      val result = repository.getRelatedTos(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Separated From" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            LabelledIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "NCB records 1963",
              new URI(s"${Cat.NS}CAB.2022.L744")
            )
          )
        )
      )
      val result = repository.getSeparatedFroms(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
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
      )
      val result = repository.getSeparatedFroms(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get URI Subjects" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[IdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            IdentifierEntity(new URI(s"${Cat.NS}COAL.2022.N373.P.2"), new URI(s"${Cat.NS}agent.4N6"))
          )
        )
      )
      val result = repository.getUriSubjects(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[IdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            IdentifierEntity(new URI(s"${Cat.NS}COAL.2022.N373.P.2"), new URI(s"${Cat.NS}agent.4N6")),
            IdentifierEntity(new URI(s"${Cat.NS}COAL.2022.N373.P.2"), new URI(s"${Cat.NS}agent.S7"))
          )
        )
      )
      val result = repository.getUriSubjects(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

  "Get Labelled Subjects" - {
    "must return a Success with a list of one item" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            LabelledIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "from 1965",
              new URI(s"${Cat.NS}agent.24")
            )
          )
        )
      )
      val result = repository.getLabelledSubjects(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 1
    }
    "must return a Success with a list of two items" in {
      when(mockConnector.execute[LabelledIdentifierEntity](any, any)).thenReturn(
        Try(
          List(
            LabelledIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "from 1965",
              new URI(s"${Cat.NS}agent.24")
            ),
            LabelledIdentifierEntity(
              new URI(s"${Cat.NS}COAL.2022.N373.P.2"),
              "from 1968",
              new URI(s"${Cat.NS}agent.S7")
            )
          )
        )
      )
      val result = repository.getLabelledSubjects(s"${Cat.NS}COAL.2022.N373.P")
      result.success.get.length mustBe 2
    }
  }

}
