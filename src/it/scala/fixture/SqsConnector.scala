package fixture

//import com.amazon.sqs.javamessaging.{ProviderConfiguration, SQSConnection, SQSConnectionFactory}
//import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials}
//import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
//import com.amazonaws.regions.Regions
//import com.amazonaws.services.sqs.AmazonSQSClientBuilder

import com.amazon.sqs.javamessaging.{ProviderConfiguration, SQSConnectionFactory}
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClientBuilder

import javax.jms.Connection

/** Setup a JMS connection using the SQSConnectionFactory Client
  *
  * @param serviceEndpoint
  *   the endpoint of the queue
  */
case class SqsConnector(serviceEndpoint: String) extends Connector {

  private val credentialsProvider: AWSCredentialsProvider = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials("x", "x")
  )

  private val endpointConfiguration = new EndpointConfiguration(serviceEndpoint, Regions.EU_WEST_1.getName)

  private val connectionFactory = new SQSConnectionFactory(
    new ProviderConfiguration(),
    AmazonSQSClientBuilder.standard
      .withEndpointConfiguration(endpointConfiguration)
      .withCredentials(credentialsProvider)
  )

  private val connection: SQSConnection = connectionFactory.createConnection

  override def getConnection: Connection = connection

  override def queueExists(queueName: String): Boolean = {
    val client = connection.getWrappedAmazonSQSClient
    client.queueExists(queueName)
  }

}
