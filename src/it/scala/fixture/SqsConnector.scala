package fixture

//import com.amazon.sqs.javamessaging.{ProviderConfiguration, SQSConnection, SQSConnectionFactory}
//import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials}
//import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
//import com.amazonaws.regions.Regions
//import com.amazonaws.services.sqs.AmazonSQSClientBuilder

import com.amazon.sqs.javamessaging.{ ProviderConfiguration, SQSConnection, SQSConnectionFactory }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, AwsCredentialsProvider, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.endpoints.{ SqsEndpointParams, SqsEndpointProvider }

import java.net.URI
//import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials}
//import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
//import com.amazonaws.regions.Regions
//import com.amazonaws.services.sqs.AmazonSQSClientBuilder

import javax.jms.Connection

/** Setup a JMS connection using the SQSConnectionFactory Client
  *
  * @param serviceEndpoint
  *   the endpoint of the queue
  */
case class SqsConnector(serviceEndpoint: String) extends Connector {

//  private val credentialsProvider: AwsCredentialsProvider = new StaticCredentialsProvider(
//    new AwsBasicCredentials("x", "x")
//  )

  private val credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x"))

  // private val endpointParams = SqsEndpointParams.builder().endpoint(serviceEndpoint).region(Region.EU_WEST_1)

  // val client = AmazonSQSClientBuilder

  // private val endpointConfiguration = new EndpointConfiguration(serviceEndpoint, Region.EU_WEST_1.getName)

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

  override def queueExists(queueName: String): Boolean = {
    val client = connection.getWrappedAmazonSQSClient
    client.queueExists(queueName)
  }

}
