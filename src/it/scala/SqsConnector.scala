import com.amazon.sqs.javamessaging.{ ProviderConfiguration, SQSConnection, SQSConnectionFactory }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

import java.net.URI
import javax.jms.Connection

/** Setup a JMS connection using the SQSConnectionFactory Client
  *
  * @param serviceEndpoint
  *   the endpoint of the queue
  */
case class SqsConnector(serviceEndpoint: String) extends Connector {

  private val credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x"))

  private val connectionFactory = new SQSConnectionFactory(
    new ProviderConfiguration(),
    SqsClient
      .builder()
      .credentialsProvider(credentialsProvider)
      .endpointOverride(new URI(serviceEndpoint))
      .region(Region.EU_WEST_1)
  )

  private val connection: SQSConnection = connectionFactory.createConnection

  override def getConnection: Connection = connection

}
