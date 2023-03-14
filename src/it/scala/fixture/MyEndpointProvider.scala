package fixture

import software.amazon.awssdk.endpoints.Endpoint
import software.amazon.awssdk.services.sqs.endpoints.{ SqsEndpointParams, SqsEndpointProvider }

import java.util.concurrent.CompletableFuture

class MyEndpointProvider extends SqsEndpointProvider {
  override def resolveEndpoint(endpointParams: SqsEndpointParams): CompletableFuture[Endpoint] = ???
}
