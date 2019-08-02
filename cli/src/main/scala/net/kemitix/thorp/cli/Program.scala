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
      plan    <- PlanBuilder.createPlan
      archive <- UnversionedMirrorArchive.default(plan.syncTotals)
      events  <- applyPlan(archive, plan)
      _       <- SyncLogging.logRunFinished(events)
    } yield ()

  private def handleErrors(throwable: Throwable) =
    Console.putStrLn("There were errors:") *> logValidationErrors(throwable)

  private def logValidationErrors(throwable: Throwable) =
    throwable match {
      case ConfigValidationException(errors) =>
        ZIO.foreach_(errors)(error => Console.putStrLn(s"- $error"))
    }

  private def applyPlan(archive: ThorpArchive, syncPlan: SyncPlan) =
    ZIO
      .foldLeft(sequenceActions(syncPlan.actions))(
        EventQueue(Stream.empty, syncPlan.syncTotals.totalSizeBytes))(
        applyAction(archive)(_, _))
      .map(_.events)

  private def sequenceActions(actions: Stream[Action]) =
    actions.zipWithIndex
      .map({ case (a, i) => SequencedAction(a, i) })

  private def applyAction(archive: ThorpArchive)(
      queue: EventQueue,
      action: SequencedAction
  ) = {
    val remainingBytes = queue.bytesInQueue - action.action.size
    archive
      .update(action, remainingBytes)
      .map(events => EventQueue(queue.events ++ Stream(events), remainingBytes))
  }

}

object Program extends Program
