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

import cats.effect.kernel.Fiber
import cats.effect.std.Supervisor
import cats.effect.{ExitCode, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FutureOutcome, TryValues}
import org.scalatest.freespec.{AsyncFreeSpec, FixtureAsyncFreeSpec}
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.{LocalMessageSupport, MockApp, MockService}

import scala.concurrent.duration.DurationInt

class MockServiceSpec extends AsyncFreeSpec with BeforeAndAfterAll with AsyncIOSpec with Matchers with TryValues with LocalMessageSupport with MockitoSugar {

  "dsadasda" - {
    "dsadas" in {
      val service = new MockService(false,false)
      val serviceIO = service.start
      val result = for {
        fiber <- serviceIO.start
        _ <- IO.sleep(10.seconds)
        outcome <- fiber.cancel
      } yield outcome
      result.asserting(_ mustBe ())
    }
  }


}
