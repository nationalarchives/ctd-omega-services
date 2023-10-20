import cats.effect.testing.scalatest.AsyncIOSpec
import org.apache.jena.ext.xerces.util.URI
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Second, Seconds, Span }
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.messages.request.ListAgentSummary
import uk.gov.nationalarchives.omega.api.repository.OmegaRepository
import uk.gov.nationalarchives.omega.api.repository.vocabulary.Cat

class OmegaRepositoryISpec
    extends AsyncFreeSpec with AsyncIOSpec with Matchers with Eventually with IntegrationPatience with BeforeAndAfterAll
    with MockitoSugar {

  private val config = TestServiceConfig()
  private val connector = new SparqlEndpointConnector(config)
  private val repository = new OmegaRepository(connector)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(30, Seconds)), interval = scaled(Span(1, Second)))

  override protected def beforeAll(): Unit =
    BulkLoadData.createRepository().unsafeRunSync()

  "Get Legal Status summaries" - {

    "must return a Success with 5 legal status items" in {
      eventually {
        val result = repository.getLegalStatusEntities
        result.asserting(_.length mustBe 5)
      }
    }
  }

  "Get List Agent Entities" - {

    "must return a List of 24 AgentEntity items" in {
      eventually {
        val result = repository.getAgentSummaryEntities(ListAgentSummary())
        result.asserting(_.length mustBe 24)
      }
    }
    "must return a List of 2 places of deposit AgentEntity items" in {
      eventually {
        val result = repository.getAgentSummaryEntities(ListAgentSummary(depository = Some(true)))
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get List Agent Descriptions" - {

    "must return a List with one item" in {
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(),
          new URI(s"${Cat.NS}agent.HHC")
        )
        result.asserting(_.length mustBe 1) *>
          result.asserting(_.head.identifier mustBe s"${Cat.NS}agent.HHC.2")
      }
    }
    "must return a List containing the latest HHC agent (HH2)" in {
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("latest")),
          new URI(s"${Cat.NS}agent.HHC")
        )
        result.asserting(_.length mustBe 1) *>
          result.asserting(_.head.identifier mustBe s"${Cat.NS}agent.HHC.2")
      }
    }
    "must return a List with both HHC agent descriptions" in {
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("all")),
          new URI(s"${Cat.NS}agent.HHC")
        )
        result.asserting(_.length mustBe 2)
      }
    }
    "must return a List containing with all HHC agents created from a date onwards (0)" in {
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("2023-08-01T00:00:00.000Z")),
          new URI(s"${Cat.NS}agent.HHC")
        )
        result.asserting(_ mustEqual List.empty)
      }
    }
    "must return a List containing with all HHC agents created from a date onwards (1)" in {
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("2023-07-01T00:00:00.000Z")),
          new URI(s"${Cat.NS}agent.HHC")
        )
        result.asserting(_.head.identifier mustBe s"${Cat.NS}agent.HHC.2")
      }
    }
    "must return a List containing with all HHC agents created from a date onwards (2)" in {
      eventually {
        val result = repository.getAgentDescriptionEntities(
          ListAgentSummary(versionTimestamp = Some("2023-01-01T00:00:00.000Z")),
          new URI(s"${Cat.NS}agent.HHC")
        )
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Record Concept Entity" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getRecordConceptEntity("COAL.2022.N373.P")
        result.asserting(_.length mustBe 1) *>
          result.asserting(_.head.currentDescriptionUri mustEqual new URI(s"${Cat.NS}COAL.2022.N373.P.2"))
      }
    }
  }

  "Get Creator Entity" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getCreatorEntities(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 1) *>
          result.asserting(_.head.identifier mustEqual new URI(s"${Cat.NS}agent.24")) *>
          result.asserting(_.head.label mustBe "from 1965")
      }
    }
  }

  "Get Record Description Summaries" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getRecordDescriptionSummaries(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 1)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getRecordDescriptionSummaries(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Record Description Properties" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getRecordDescriptionProperties(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 1)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getRecordDescriptionProperties(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Access Rights" - {
    "must return a List with two items" in {
      eventually {
        val result = repository.getAccessRights(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 2)
      }
    }
    "must return a List with four items" in {
      eventually {
        val result = repository.getAccessRights(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 4)
      }
    }
  }

  "Get Is Part Of" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getIsPartOf(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 1)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getIsPartOf(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Secondary Identifiers" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getSecondaryIdentifiers(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 1)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getSecondaryIdentifiers(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Is Referenced By" - {
    "must return an empty List" in {
      eventually {
        val result = repository.getIsReferencedBys(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 0)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getIsReferencedBys(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Related To" - {
    "must return an empty List" in {
      eventually {
        val result = repository.getRelatedTos(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 0)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getRelatedTos(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Separated From" - {
    "must return an empty List" in {
      eventually {
        val result = repository.getSeparatedFroms(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 0)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getSeparatedFroms(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get URI Subjects" - {
    "must return an empty List" in {
      eventually {
        val result = repository.getUriSubjects(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 0)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getUriSubjects(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

  "Get Labelled Subjects" - {
    "must return a List with one item" in {
      eventually {
        val result = repository.getLabelledSubjects(s"${Cat.NS}COAL.2022.N36R.P")
        result.asserting(_.length mustBe 1)
      }
    }
    "must return a List with two items" in {
      eventually {
        val result = repository.getLabelledSubjects(s"${Cat.NS}COAL.2022.N373.P")
        result.asserting(_.length mustBe 2)
      }
    }
  }

}
