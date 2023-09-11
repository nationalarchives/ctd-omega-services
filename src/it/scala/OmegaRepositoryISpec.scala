import cats.effect.unsafe.implicits.global
import org.apache.jena.ext.xerces.util.URI
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Second, Seconds, Span }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.{ BaseURL, OmegaRepository }

class OmegaRepositoryISpec
    extends AnyFreeSpec with Matchers with Eventually with IntegrationPatience with BeforeAndAfterAll
    with MockitoSugar {

  private val mockConfig = mock[ServiceConfig]
  private val connector = new SparqlEndpointConnector(mockConfig)
  private val repository = new OmegaRepository(connector)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(30, Seconds)), interval = scaled(Span(1, Second)))

  override protected def beforeAll(): Unit =
    BulkLoadData.createRepository().unsafeRunSync()

  "Get Legal Status summaries" - {

    "must return a Success with 5 legal status items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getLegalStatusEntities
        result.success.get.length mustBe 5
      }
    }
  }

  "Get List Agent Entities" - {

    "must return a List of 24 AgentEntity items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentSummaryEntities(ListAgentSummary())
        result.success.get.length mustBe 24
      }
    }
    "must return a List of 2 places of deposit AgentEntity items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentSummaryEntities(ListAgentSummary(depository = Some(true)))
        result.success.get.length mustBe 2
      }
    }
  }

  "Get List Agent Descriptions" - {

    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(),
          new URI(s"${BaseURL.cat}/agent.HHC")
        )
        result.success.get.length mustBe 1
        result.success.get.head.identifier mustBe s"${BaseURL.cat}/agent.HHC.2"
      }
    }
    "must return a List containing the latest HHC agent (HH2)" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("latest")),
          new URI(s"${BaseURL.cat}/agent.HHC")
        )
        result.success.get.length mustBe 1
        result.success.get.head.identifier mustBe s"${BaseURL.cat}/agent.HHC.2"
      }
    }
    "must return a List with both HHC agent descriptions" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("all")),
          new URI(s"${BaseURL.cat}/agent.HHC")
        )
        result.success.get.length mustBe 2
      }
    }
    "must return a List containing with all HHC agents created from a date onwards (0)" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("2023-08-01T00:00:00.000Z")),
          new URI(s"${BaseURL.cat}/agent.HHC")
        )
        result.success.get mustEqual List.empty
      }
    }
    "must return a List containing with all HHC agents created from a date onwards (1)" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("2023-07-01T00:00:00.000Z")),
          new URI(s"${BaseURL.cat}/agent.HHC")
        )
        result.success.get.head.identifier mustBe s"${BaseURL.cat}/agent.HHC.2"
      }
    }
    "must return a List containing with all HHC agents created from a date onwards (2)" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("2023-01-01T00:00:00.000Z")),
          new URI(s"${BaseURL.cat}/agent.HHC")
        )
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Record Concept Entity" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRecordConceptEntity("COAL.2022.N373.P")
        result.success.get.length mustBe 1
        result.success.get.head.currentDescriptionUri mustEqual new URI(s"${BaseURL.cat}/COAL.2022.N373.P.2")
      }
    }
  }

  "Get Creator Entity" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getCreatorEntities(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 1
        result.success.get.head.identifier mustEqual new URI(s"${BaseURL.cat}/agent.24")
        result.success.get.head.label mustBe "from 1965"
      }
    }
  }

  "Get Record Description Summaries" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRecordDescriptionSummaries(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 1
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRecordDescriptionSummaries(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Record Description Properties" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRecordDescriptionProperties(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 1
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRecordDescriptionProperties(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Access Rights" - {
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAccessRights(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 2
      }
    }
    "must return a List with four items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAccessRights(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 4
      }
    }
  }

  "Get Is Part Of" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getIsPartOf(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 1
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getIsPartOf(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Secondary Identifiers" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getSecondaryIdentifiers(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 1
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getSecondaryIdentifiers(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Is Referenced By" - {
    "must return an empty List" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getIsReferencedBys(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 0
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getIsReferencedBys(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Related To" - {
    "must return an empty List" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRelatedTos(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 0
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getRelatedTos(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Separated From" - {
    "must return an empty List" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getSeparatedFroms(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 0
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getSeparatedFroms(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get URI Subjects" - {
    "must return an empty List" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getUriSubjects(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 0
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getUriSubjects(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

  "Get Labelled Subjects" - {
    "must return a List with one item" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getLabelledSubjects(s"${BaseURL.cat}/COAL.2022.N36R.P")
        result.success.get.length mustBe 1
      }
    }
    "must return a List with two items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getLabelledSubjects(s"${BaseURL.cat}/COAL.2022.N373.P")
        result.success.get.length mustBe 2
      }
    }
  }

}
