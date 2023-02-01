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
import com.monovore.decline.{ Command, Help, Opts }
import cats.implicits._
import uk.gov.nationalarchives.omega.api.common.ServiceArgs

/** A command line parser based on Decline (see https://ben.kirw.in/decline/) N.B. BuildInfo is generated on compile and
  * will be available in the ./target/scala-2.13/src_managed directory
  */
sealed trait OmegaCommand

object OmegaCommand {

  sealed trait ApiSubcommand
  case class RunCommand(args: ServiceArgs) extends ApiSubcommand
  case class ApiCommand(subcommand: ApiSubcommand) extends OmegaCommand

  case object ShowVersion extends OmegaCommand

  private val diskStoreOpt =
    Opts
      .option[String](
        "disk-store",
        short = "d",
        help = "The full path to a directory for temporary disk storage of messages."
      )
      .orNone

  val configFileOpt: Opts[Option[String]] =
    Opts
      .option[String](
        "config-file",
        short = "c",
        help = "The full path to a file containing a custom configuration for the application."
      )
      .orNone

  val args: Opts[ServiceArgs] = (diskStoreOpt, configFileOpt).mapN(ServiceArgs.apply)

  private val runCommand = Opts.subcommand("start", "Starts the application.")(args.map(RunCommand))

  private val filename = Opts.option[String]("filename", short = "f", help = "The API docs filename.")

  private val allSubCommand = runCommand

  private val apiSubcommand: Opts[OmegaCommand] = allSubCommand.map(ApiCommand)

  private val showVersion: Opts[OmegaCommand] =
    Opts.flag("version", "Show version and exit.").map(_ => ShowVersion)

  private val omega: Command[OmegaCommand] =
    Command(BuildInfo.name, "Pan-Archival Catalogue Services Prototype")(showVersion.orElse(apiSubcommand))

  /** Parses the command line arguments and returns either the requested OmegaCommand or the Help
    * @param args
    *   the command line arguments
    * @return
    */
  def parse(args: Seq[String]): Either[Help, OmegaCommand] =
    omega.parse(args)

}
