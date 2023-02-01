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

import buildinfo.BuildInfo
import cats.effect.{ ExitCode, IO, IOApp }
import uk.gov.nationalarchives.omega.api.OmegaCommand.{ ApiCommand, RunCommand, ShowVersion }
import uk.gov.nationalarchives.omega.api.services.ApiService

/** This is the main entry point to the application and is responsible for handling command line input, together with
  * the uk.gov.nationalarchives.omega.api.OmegaCommand
  *
  * N.B. BuildInfo is generated on compile and will be available in the ./target/scala-2.13/src_managed directory
  */
object ApiServiceApp extends IOApp {

  // see https://tldp.org/LDP/abs/html/exitcodes.html
  private val EXIT_SUCCESS = 0
  private val EXIT_GENERAL_ERROR = 1

  override def run(args: List[String]): IO[ExitCode] =
    OmegaCommand.parse(args.toIndexedSeq) match {
      case Left(help) if help.errors.nonEmpty =>
        // user needs help due to errors with expected args
        System.err.println(help)
        sys.exit(EXIT_GENERAL_ERROR)

      case Left(help) =>
        // help was requested by the user, i.e.: `--help`
        println(help)
        sys.exit(EXIT_SUCCESS)

      case Right(ApiCommand(RunCommand(apiConfig))) =>
        val apiService = ApiService(apiConfig)

        // install a shutdown hook on the API Service so that when the App receives SIGTERM it stops gracefully
        sys.ShutdownHookThread {
          apiService.stop()
        }

        // start the API Service
        apiService.start

      case Right(ShowVersion) =>
        println(BuildInfo.version)
        sys.exit(EXIT_SUCCESS)
    }

}
