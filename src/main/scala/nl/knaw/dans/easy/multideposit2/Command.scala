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

import java.nio.file.Paths

import nl.knaw.dans.easy.multideposit2.PathExplorer.PathExplorers
import nl.knaw.dans.lib.error.TryExtensions
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {

  type FeedBackMessage = String

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration.version)
  val app = SplitMultiDepositApp(configuration)

  app.acquireAndGet(runSubcommand)
    .doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }

  private def runSubcommand(app: SplitMultiDepositApp): Try[FeedBackMessage] = {
    commandLine.subcommand
      .collect {
        case ingest @ commandLine.ingest =>
          val sd = ingest.stagingDir.toOption
            .getOrElse(Paths.get(configuration.properties.getString("staging-dir")))
          val md = ingest.multiDepositDir()
          val od = ingest.outputDepositDir()
          val dm = ingest.datamanager()

          app.convert(new PathExplorers(sd, md, od), dm)
            .map(_ => s"Finished successfully! The output can be found in $od.")
        case commandLine.runService => runAsService(app)
      }
      .getOrElse(Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }")))
  }

  private def runAsService(app: SplitMultiDepositApp): Try[FeedBackMessage] = Try {
    //    val service = new PidGeneratorService(configuration.properties.getInt("pid-generator.daemon.http.port"), app,
    //      "/" -> new PidGeneratorServlet(app, configuration))
    //    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
    //      override def run(): Unit = {
    //        service.stop()
    //        service.destroy()
    //      }
    //    })
    //
    //    service.start()
    //    Thread.currentThread.join()
    "Service terminated normally."
  }
}
