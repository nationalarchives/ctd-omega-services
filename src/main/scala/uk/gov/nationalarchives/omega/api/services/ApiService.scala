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

package uk.gov.nationalarchives.omega.api.services

import cats.effect.std.Queue
import cats.effect.{ ExitCode, IO }
import cats.implicits.catsSyntaxParallelTraverse_
import jms4s.JmsAcknowledgerConsumer.AckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.{ JmsAcknowledgerConsumer, JmsProducer }
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{ LoggerFactory, SelfAwareStructuredLogger }
import pureconfig.generic.auto._
import pureconfig.{ ConfigObjectSource, ConfigSource }
import uk.gov.nationalarchives.omega.api.common.ServiceArgs
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.connectors.JmsConnector
import uk.gov.nationalarchives.omega.api.services.LocalMessageStore.PersistentMessageId
import uk.gov.nationalarchives.omega.api.services.ServiceState.{ Started, Starting, Stopped, Stopping }

import java.nio.file.{ Files, Paths }
import java.util.concurrent.atomic.AtomicReference

class ApiService(val config: ServiceConfig) {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  private var errorOnInvalidServiceState = false
  private val state = new AtomicReference[ServiceState](Stopped)

  private val localMessageStore = {
    val messageStoreFolder = Paths.get(config.tempMessageDir)
    Files.createDirectories(messageStoreFolder)
    new LocalMessageStore(messageStoreFolder)
  }

  def start: IO[ExitCode] =
    // check if we can start the service
    if (!switchState(Stopped, Starting)(config.errorOnInvalidServiceState)) {
      IO.pure(ExitCode.Error)
    } else {
      attemptStartUp()
    }

  private def attemptStartUp(): IO[ExitCode] = {
    // copy config option to local state
    this.errorOnInvalidServiceState = config.errorOnInvalidServiceState
    try {
      val ec = doStart(config)
      // switch to the started state
      switchState(Starting, Started)(errorOnInvalidServiceState)
      ec
    } catch {
      case e: Throwable =>
        // we are in some inconsistent state as startup failed, so we must attempt shutdown
        try {
          doStop()
          switchState(Starting, Stopped)(errorOnInvalidServiceState)
          IO.pure(ExitCode.Error)
        } finally
          logger.error(e.getMessage)
    }
  }

  @throws[IllegalStateException]
  def stop(): Unit = {
    // check if we can stop the service
    if (switchState(Started, Stopping)(errorOnInvalidServiceState)) {
      logger.info("Closing connection..")
      if (switchState(Stopping, Stopped)(errorOnInvalidServiceState)) {
        doStop()
      }
    }
    () // Explicitly return unit
  }

  @throws[IllegalStateException]
  private def switchState(from: ServiceState, to: ServiceState)(errorOnInvalidServiceState: Boolean): Boolean = {
    val switched = state.compareAndSet(from, to)
    if (!switched) {
      val msg = s"Unable to switch ApiService state from: $from, to: $to."
      if (errorOnInvalidServiceState) {
        throw new IllegalStateException(msg)
      } else {
        logger.warn(msg)
      }
    }
    switched
  }

  private def doStart(config: ServiceConfig): IO[ExitCode] = {

    // TODO(AR) - one client, how to ack a consumer message after local persistence and then process it, and then produce a response
    // TODO(AR) how to wire up queues and services using a config file or DSL?

    // TODO(AR) request queue will typically be 1 (plus maybe a few more for expedited ops), response queues will be per external application

    val localQueue: IO[Queue[IO, LocalMessage]] = Queue.bounded[IO, LocalMessage](config.maxLocalQueueSize)
    val jmsConnector = new JmsConnector(config)

    val result = for {
      q <- localQueue
      res <-
        jmsConnector.getJmsProducerAndConsumer(QueueName(config.requestQueue)).use { case (jmsProducer, jmsConsumer) =>
          process(q, jmsConsumer, jmsProducer)
        }
    } yield res
    result.as(ExitCode.Success)
  }

  private def process(
    queue: Queue[IO, LocalMessage],
    consumer: JmsAcknowledgerConsumer[IO],
    producer: JmsProducer[IO]
  ) =
    IO.race(
      createMessageHandler(queue)(consumer),
      List.range(start = 0, end = config.maxDispatchers).parTraverse_ { i =>
        logger.info(s"Starting consumer #${i + 1}") >>
          new Dispatcher(new LocalProducerImpl(producer, QueueName(config.replyQueue))).run(i)(queue).foreverM
      }
    )

  private def doStop(): Unit = {
    // TODO(RW) this is where we will need to close any connections, for example to SQS or OpenSearch
    logger.info("Connection closed.")
  }

  private def acknowledgeMessage(): IO[AckAction[IO]] =
    logger.info("Acknowledged message") *> IO(AckAction.ack)

  @throws[IllegalArgumentException]
  private def createLocalMessage(persistentMessageId: PersistentMessageId, jmsMessage: JmsMessage): IO[LocalMessage] =
    for {
      sid <-
        IO.fromOption(jmsMessage.getStringProperty("sid"))(throw new IllegalArgumentException("Missing service ID"))
      serviceId <-
        IO.fromOption(ServiceIdentifier.withNameOption(sid))(throw new IllegalArgumentException("SID not recognised"))
      messageId <- IO.fromOption(jmsMessage.getJMSMessageId)(throw new IllegalArgumentException("Missing message ID"))
      text      <- jmsMessage.asTextF[IO]
    } yield LocalMessage(persistentMessageId, serviceId, text, messageId)

  private def createMessageHandler(
    queue: Queue[IO, LocalMessage]
  )(jmsAcknowledgerConsumer: JmsAcknowledgerConsumer[IO]): IO[Unit] =
    jmsAcknowledgerConsumer.handle { (jmsMessage, _) =>
      for {
        persistentMessageId <- localMessageStore.persistMessage(jmsMessage)
        res                 <- acknowledgeMessage()
        _                   <- queueMessage(queue, persistentMessageId, jmsMessage)
      } yield res
    }

  private def queueMessage(
    queue: Queue[IO, LocalMessage],
    persistentMessageId: PersistentMessageId,
    jmsMessage: JmsMessage
  ): IO[Unit] =
    for {
      localMessageResult <- createLocalMessage(persistentMessageId, jmsMessage).attempt
      _ <- localMessageResult match {
             case Left(e)  => logger.error(s"Failed to queue message due to ${e.getMessage}")
             case Right(m) => queue.offer(m) *> logger.info(s"Queued message: $persistentMessageId")
           }
    } yield ()

}
object ApiService {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger
  def apply(args: ServiceArgs): ApiService = {
    val serviceConfig = loadConfig(args)
    new ApiService(serviceConfig)
  }

  private def loadConfig(serviceArgs: ServiceArgs): ServiceConfig = {
    val maybeTempMessageDir = serviceArgs.tempMessageDir.flatMap(dir => Some(s"{ temp-message-dir = $dir }"))
    val defaultConfigSource: ConfigObjectSource = ConfigSource.resources("application.conf")
    val configFilePath: String = serviceArgs.configFilePath.getOrElse("")
    logger.info(s"Starting application with diskStorePath=${serviceArgs.tempMessageDir}")
    if (configFilePath.nonEmpty && Files.exists(Paths.get(configFilePath))) {
      maybeTempMessageDir match {
        case Some(tempMessageDir) =>
          ConfigSource
            .string(tempMessageDir)
            .withFallback(ConfigSource.file(configFilePath))
            .withFallback(defaultConfigSource)
            .loadOrThrow[ServiceConfig]
        case None => ConfigSource.file(configFilePath).withFallback(defaultConfigSource).loadOrThrow[ServiceConfig]
      }
    } else {
      maybeTempMessageDir match {
        case Some(tempMessageDir) =>
          ConfigSource.string(tempMessageDir).withFallback(defaultConfigSource).loadOrThrow[ServiceConfig]
        case None => defaultConfigSource.loadOrThrow[ServiceConfig]
      }
    }
  }

}
