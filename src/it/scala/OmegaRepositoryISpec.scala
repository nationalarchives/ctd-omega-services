import cats.effect.unsafe.implicits.global
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{ Second, Seconds, Span }
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.repository.OmegaRepository

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
        val result = repository.getLegalStatusSummaries
        result.success.get.length mustBe 5
      }
    }
  }

  "Get List Agent summaries" - {

    "must return a List of 24 AgentSummary items" in {
      when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
      eventually {
        val result = repository.getAgentSummaries
        result.success.get.length mustBe 24
      }

    }
  }
}
