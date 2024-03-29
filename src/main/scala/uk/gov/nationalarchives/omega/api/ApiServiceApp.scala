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

import cats.effect.std.Supervisor
import cats.effect.{ ExitCode, IO, IOApp }
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import uk.gov.nationalarchives.omega.api.common.AppLogger
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig
import uk.gov.nationalarchives.omega.api.services.{ ApiService, InvalidStateException }

object ApiServiceApp extends IOApp with AppLogger {

  val applicationId = "PACS001"
  private val invalidStateExitCode = 99

  override def run(args: List[String]): IO[ExitCode] = {
    val serviceConfig = ConfigSource.default.loadOrThrow[ServiceConfig]
    val apiService = new ApiService(serviceConfig)
    val apiServiceSuspended = apiService.startSuspended
    Supervisor[IO](await = true).use { supervisor =>
      for {
        fiber   <- supervisor.supervise(apiServiceSuspended)
        outcome <- fiber.join
      } yield outcome.fold(ExitCode.Success, getErrorExitCode, _ => ExitCode.Success)
    }
  }

  private def getErrorExitCode(throwable: Throwable) =
    throwable match {
      case _: InvalidStateException => ExitCode(invalidStateExitCode)
      case _                        => ExitCode.Error
    }

}
