/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.multideposit2

import java.nio.file.{ Path, Paths }

import nl.knaw.dans.easy.multideposit.FileExtensions
import nl.knaw.dans.easy.multideposit2.model.Datamanager
import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand }

class CommandLineOptions(args: Array[String], version: String) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-split-multi-deposit"
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description = "Splits a Multi-Deposit into several deposit directories for subsequent ingest into the archive"
  val synopsis = s"""$printedName.sh [{--staging-dir|-s} <dir>] <multi-deposit-dir> <output-deposits-dir> <datamanager>"""

  version(s"$printedName v$version")
  banner(
    s"""
       |  $description
       |  Utility to process a Multi-Deposit prior to ingestion into the DANS EASY Archive
       |
       |Usage:
       |
       |  $synopsis
       |
       |Options:
       |""".stripMargin)

  val runService = new Subcommand("run-service") {
    descr("Starts the EASY Split Multi Deposit as a daemon that services HTTP requests")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(runService)

  val ingest = new Subcommand("ingest") {
    val stagingDir: ScallopOption[Path] = opt[Path](
      name = "staging-dir",
      short = 's',
      descr = "A directory in which the deposit directories are created, after which they will be " +
        "moved to the 'output-deposit-dir'. If not specified, the value of 'staging-dir' in " +
        "'application.properties' is used.")

    val multiDepositDir: ScallopOption[Path] = trailArg[Path](
      name = "multi-deposit-dir",
      required = true,
      descr = "Directory containing the Submission Information Package to process. "
        + "This must be a valid path to a directory containing a file named "
        + s"'${ PathExplorer.instructionsFileName }' in RFC4180 format.")

    val outputDepositDir: ScallopOption[Path] = trailArg[Path](
      name = "output-deposit-dir",
      required = true,
      descr = "A directory to which the deposit directories are moved after the staging has been " +
        "completed successfully. The deposit directory layout is described in the easy-sword2 " +
        "documentation")

    val datamanager: ScallopOption[Datamanager] = trailArg[Datamanager](
      name = "datamanager",
      required = true,
      descr = "The username (id) of the datamanger (archivist) performing this deposit")

    validatePathExists(multiDepositDir)
    validateFileIsDirectory(multiDepositDir.map(_.toFile))
    validate(multiDepositDir)(dir => {
      val instructionFile: Path = PathExplorer.multiDepositInstructionsFile(dir)
      if (!dir.directoryContains(instructionFile))
        Left(s"No instructions file found in this directory, expected: $instructionFile")
      else
        Right(())
    })

    validatePathExists(outputDepositDir)
    validateFileIsDirectory(outputDepositDir.map(_.toFile))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(ingest)

  verify()
}
