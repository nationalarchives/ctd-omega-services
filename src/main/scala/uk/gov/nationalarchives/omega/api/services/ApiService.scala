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
import cats.effect.{ ExitCode, IO, Resource }
import cats.implicits.catsSyntaxParallelTraverse_
import jms4s.JmsAcknowledgerConsumer.AckAction
import jms4s.config.QueueName
import jms4s.jms.JmsMessage
import jms4s.{ JmsAcknowledgerConsumer, JmsProducer }
import uk.gov.nationalarchives.omega.api.business.echo.EchoService
import uk.gov.nationalarchives.omega.api.business.legalstatus.LegalStatusService
import uk.gov.nationalarchives.omega.api.common.Version1UUID
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.connectors.JmsConnector
import uk.gov.nationalarchives.omega.api.messages.{ LocalMessage, LocalMessageStore, StubDataImpl }
import uk.gov.nationalarchives.omega.api.messages.LocalMessage.createLocalMessage
import uk.gov.nationalarchives.omega.api.services.ServiceState.{ Started, Starting, Stopped, Stopping }

import java.nio.file.{ Files, Paths }
import scala.util.{ Failure, Success }

class ApiService(val config: ServiceConfig) extends Stateful {

  def start: IO[ExitCode] =
    switchState(Stopped, Starting).ifM(attemptStartUp(), IO.pure(ExitCode(invalidState)))

  private def attemptStartUp(): IO[ExitCode] =
    switchState(Starting, Started)
      .ifM(
        doStart(),
        IO.pure(ExitCode(invalidState))
      )
      .handleErrorWith(error =>
        logger.error(s"Shutting down due to ${error.getMessage}") *>
          doStop() *> switchState(Starting, Stopped) *> IO.pure(ExitCode.Error)
      )

  def stop(): IO[Unit] =
    switchState(Started, Stopping).ifM(
      logger.info("Closing connection..") *>
        switchState(Stopping, Stopped).ifM(doStop(), IO.unit),
      IO.unit
    )

  private def doStart(): IO[ExitCode] = {

    // TODO(AR) - one client, how to ack a consumer message after local persistence and then process it, and then produce a reply
    // TODO(AR) how to wire up queues and services using a config file or DSL?
    // TODO(AR) request queue will typically be 1 (plus maybe a few more for expedited ops), reply queues will be per external application
    val localQueue: IO[Queue[IO, LocalMessage]] = Queue.bounded[IO, LocalMessage](config.maxLocalQueueSize)
    val jmsConnector = new JmsConnector(config)
    val stubData = new StubDataImpl()

    getLocalMessageStore.flatMap {
      case Right(localMessageStore) =>
        val result = for {
          _ <- runRecovery(localMessageStore, jmsConnector, stubData)
          q <- localQueue
          res <-
            jmsConnector.getJmsProducerAndConsumer(QueueName(config.requestQueue)).use {
              case (jmsProducer, jmsConsumer) =>
                startHandlerAndDispatchers(q, jmsConsumer, jmsProducer, localMessageStore, stubData)
            }
        } yield res
        result.as(ExitCode.Success)
      case Left(exitCode) => IO.pure(exitCode)
    }
  }

  private def runRecovery(
    localMessageStore: LocalMessageStore,
    jmsConnector: JmsConnector,
    stubData: StubDataImpl
  ): IO[Unit] = {
    for {
      savedFiles <- Resource.eval(localMessageStore.readAllFilesInDirectory())
      jmsClient  <- jmsConnector.createJmsClient()
      producer   <- jmsConnector.createJmsProducer(jmsClient)(config.maxProducers)
    } yield producer match {
      case producer =>
        val dispatcher = generateDispatcher(producer, localMessageStore, stubData)
        dispatcher.runRecovery(0)(savedFiles)
    }
  }.useEval

  private def getLocalMessageStore: IO[Either[ExitCode, LocalMessageStore]] = IO {
    val messageStoreFolder = Paths.get(config.tempMessageDir)
    Files.createDirectories(messageStoreFolder)
    Right(new LocalMessageStore(messageStoreFolder))
  }.handleErrorWith(ex =>
    logger.error(s"Failed to created local message store due to ${ex.getMessage}") *> IO.pure(Left(ExitCode.Error))
  )

  /* This method uses IO.race() to run the message handler and dispatcher in parallel. The common component between the
   * message handler and the dispatcher is the queue. The handler puts messages onto queue and the dispatcher takes
   * them off. Both of these tasks need to run forever unless the application is shutdown or the an unrecoverable
   * error is encountered in one task, in which case it would terminate in error and this would cause the other to be
   * cancelled.
   */
  private def startHandlerAndDispatchers(
    queue: Queue[IO, LocalMessage],
    consumer: JmsAcknowledgerConsumer[IO],
    producer: JmsProducer[IO],
    localMessageStore: LocalMessageStore,
    stubData: StubDataImpl
  ): IO[Either[Unit, Unit]] =
    IO.race(
      createMessageHandler(queue, localMessageStore)(consumer),
      List.range(start = 0, end = config.maxDispatchers).parTraverse_ { i =>
        logger.info(s"Starting consumer #${i + 1}") >>
          generateDispatcher(producer, localMessageStore, stubData)
            .run(i)(queue)
            .foreverM
      }
    )

  private def generateDispatcher(
    jmsProducer: JmsProducer[IO],
    localMessageStore: LocalMessageStore,
    stubData: StubDataImpl
  ) =
    new Dispatcher(
      new LocalProducerImpl(jmsProducer),
      localMessageStore,
      new EchoService(),
      new LegalStatusService(stubData)
    )

  private def doStop(): IO[Unit] =
    // TODO(RW) this is where we will need to close any external connections, for example to OpenSearch
    logger.info("Connection closed.")

  private def acknowledgeMessage(): IO[AckAction[IO]] =
    logger.info("Acknowledged message") *> IO(AckAction.ack)

  private def createMessageHandler(
    queue: Queue[IO, LocalMessage],
    localMessageStore: LocalMessageStore
  )(jmsAcknowledgerConsumer: JmsAcknowledgerConsumer[IO]): IO[Unit] =
    jmsAcknowledgerConsumer.handle { (jmsMessage, _) =>
      localMessageStore.persistMessage(jmsMessage).flatMap {
        case Success(messageId) =>
          for {
            res <- acknowledgeMessage()
            _   <- queueMessage(queue, messageId, jmsMessage)
          } yield res
        case Failure(e) =>
          logger.error(s"Failed to create message handler as unable to persist message: [$e]")
          IO(AckAction.noAck)
      }
    }

  private def queueMessage(
    queue: Queue[IO, LocalMessage],
    persistentMessageId: Version1UUID,
    jmsMessage: JmsMessage
  ): IO[Unit] =
    for {
      localMessageResult <- createLocalMessage(persistentMessageId, jmsMessage)
      _                  <- queue.offer(localMessageResult) *> logger.info(s"Queued message: $persistentMessageId")
    } yield ()

}
