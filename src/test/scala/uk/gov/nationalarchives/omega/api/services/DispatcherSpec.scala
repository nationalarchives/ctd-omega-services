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

import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.testing.scalatest.AsyncIOSpec
import com.fasterxml.uuid.{ EthernetAddress, Generators }
import jms4s.config.QueueName
import org.mockito.MockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.DurationInt

class DispatcherSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with MockitoSugar {

  "When the dispatcher receives a message" - {

    "for the ECHO001 service it should reply" in {
      val testQueue = QueueName("test-queue")
      val testLocalProducer = new TestProducerImpl(testQueue)
      val dispatcher = new Dispatcher(testLocalProducer)
      val generator = Generators.timeBasedGenerator(EthernetAddress.fromInterface)
      Queue.bounded[IO, LocalMessage](1).flatMap { queue =>
        queue.offer(LocalMessage(generator.generate(), ServiceIdentifier.ECHO001, "Hello World!", "1234")) *>
          dispatcher.run(1)(queue).andWait(5.seconds) *>
          IO(testLocalProducer.message).asserting(_ mustBe "The Echo Service says: Hello World!")
      }

    }
  }

}