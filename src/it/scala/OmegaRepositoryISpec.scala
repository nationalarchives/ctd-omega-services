import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.connectors.SparqlEndpointConnector
import uk.gov.nationalarchives.omega.api.models.LegalStatus
import uk.gov.nationalarchives.omega.api.repository.OmegaRepository

import scala.util.Success

class OmegaRepositoryISpec extends AnyFreeSpec with Matchers with MockitoSugar {

  "Get Legal Status summaries" - {

    val mockConfig = mock[ServiceConfig]
    when(mockConfig.sparqlEndpoint).thenReturn("http://localhost:8080/rdf4j-server/repositories/PACT")
    val connector = new SparqlEndpointConnector(mockConfig)
    val repository = new OmegaRepository(connector)

    "must return a Success with an empty list" in {
      val result = repository.getLegalStatusSummaries
      result.success.get.length mustBe 5
    }
  }
}
