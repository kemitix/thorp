package net.kemitix.thorp.cli

import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.CoreTypes.CoreProgram
import net.kemitix.thorp.core._
import zio.ZIO

trait Program {

  lazy val version = s"Thorp v${thorp.BuildInfo.version}"

  def run(args: List[String]): CoreProgram[Unit] = {
    for {
      cli    <- CliArgs.parse(args)
      config <- ConfigurationBuilder.buildConfig(cli)
      _      <- Config.set(config)
      _      <- ZIO.when(showVersion(cli))(Console.putStrLn(version))
      _      <- ZIO.when(!showVersion(cli))(execute.catchAll(handleErrors))
    } yield ()
  }

  private def showVersion: ConfigOptions => Boolean =
    cli => ConfigQuery.showVersion(cli)

  private def execute =
    for {
      _        <- SyncLogging.logRunStart
      syncPlan <- PlanBuilder.createPlan
      archive  <- UnversionedMirrorArchive.default(syncPlan.syncTotals)
      events   <- PlanExecutor.executePlan(archive, syncPlan)
      _        <- SyncLogging.logRunFinished(events)
    } yield ()

  private def handleErrors(throwable: Throwable) =
    Console.putStrLn("There were errors:") *> logValidationErrors(throwable)

  private def logValidationErrors(throwable: Throwable) =
    throwable match {
      case ConfigValidationException(errors) =>
        ZIO.foreach_(errors)(error => Console.putStrLn(s"- $error"))
    }

}

object Program extends Program
