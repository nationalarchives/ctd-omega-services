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

package uk.gov.nationalarchives.omega.api

import cats.data.OptionT
import cats.effect.std.Supervisor
import cats.effect.{ExitCode, IO, IOApp}

import scala.concurrent.duration.DurationInt

object MockApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    // TODO(AR) flip these boolean flags to simulate startup and shutdown failures
    val service = new MockService(failStart = false, failStop = true)

    // TODO(AR) we want to:
    //  1. Start our MockService (i.e. `MockService#start`) and have it run forever. However:
    //  2. If startup fails we need to report this to the CLI and exit - InvalidStateException
    //  3. If SIGINT (e.g. Ctrl-C) is received we should shutdown our MockService (i.e. `MockService#stop`) - call stop and return success
    //  3.1 If shutdown fails we need to report this to the CLI and exit with a non-zero exit code
    //  3.2 If shutdown succeeds we need to exit OK

    // NOTE(AR) at the moment only (1) and (3.2) are working, i.e. (failStart = false, failStop = false)

    // There are only two scenarios where the application should stop:
    // 1. An unrecoverable error occurs
    // 2. The application is cancelled

    Supervisor[IO](await = true)
      .use { supervisor =>
        for {
          fiber <- supervisor.supervise(service.start)
          outcome <- fiber.join
        } yield outcome.fold(ExitCode.Success, getErrorExitCode, _ => ExitCode.Success)
      }
  }

  private def getErrorExitCode(throwable: Throwable) = {
    println(s"Error: ${throwable.getMessage}")
    throwable match {
      case _: InvalidStateException => ExitCode(99)
      case _ => ExitCode.Error
    }
  }

}


class MockService(failStart: Boolean, failStop: Boolean) {

  /** This function will either return a `Some(ErrorCondition)` relatively quickly if startup fails, or if startup
    * succeeds it will run (i.e. block) forever!
    */
  def start: IO[Unit] = {
    // 1) print
    // 2) check for failure
    // 3) sleep 10 secs
    // 4) print
    IO.println("Start has started...") >>
      IO.raiseWhen(failStart)(InvalidStateException("START FAILED")) >> IO.never.onCancel(stop()) &> IO.println(
        "Start has finished!"
      )
  }

  /** This function will either return a `Some(ErrorCondition)` relatively quickly if shutdown fails, or if shutdown
    * succeeds it will run for as long as it takes to stop the running services and will then return.
    */
  def stop(): IO[Unit] =
    IO.println("Stop has started...") >>
      IO.raiseWhen(failStop)(InvalidStateException("STOP FAILED")) <* IO.println("Stop has finished!")

}

case class InvalidStateException(message: String) extends Exception(message)
