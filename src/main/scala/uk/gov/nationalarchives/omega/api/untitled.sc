import cats.effect.IO
import cats.effect.std.Queue

import java.util.UUID

final case class LocalMessage(
  persistentMessageId: UUID,
  messageText: String,
  correlationId: Option[String]
)

val queue = Queue.bounded[IO, LocalMessage](1)

queue.map {
  q => q.offer(LocalMessage(UUID.randomUUID(),"Hello World!",Some("1234")))
}

for {
  q <- queue
  q.of
} yield println(message.messageText)