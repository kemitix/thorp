package net.kemitix.thorp.cli

import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.CoreTypes.CoreProgram
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.storage.aws.S3HashService.defaultHashService
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

  private def execute = {
    for {
      plan    <- PlanBuilder.createPlan(defaultHashService)
      archive <- UnversionedMirrorArchive.default(plan.syncTotals)
      events  <- applyPlan(archive, plan)
      _       <- SyncLogging.logRunFinished(events)
    } yield ()
  }

  private def handleErrors(throwable: Throwable) =
    for {
      _ <- Console.putStrLn("There were errors:")
      _ <- throwable match {
        case ConfigValidationException(errors) =>
          ZIO.foreach(errors)(error => Console.putStrLn(s"- $error"))
      }
    } yield ()

  private def applyPlan(
      archive: ThorpArchive,
      syncPlan: SyncPlan
  ) = {
    val zero: (Stream[StorageQueueEvent], Long) =
      (Stream(), syncPlan.syncTotals.totalSizeBytes)
    val actions = syncPlan.actions.zipWithIndex
    ZIO
      .foldLeft(actions)(zero)((acc, action) =>
        applyAction(archive, acc, action))
      .map {
        case (events, _) => events
      }
  }

  private def applyAction(
      archive: ThorpArchive,
      acc: (Stream[StorageQueueEvent], Long),
      indexedAction: (Action, Int)
  ) = {
    val (action, index)     = indexedAction
    val (stream, bytesToDo) = acc
    val remainingBytes      = bytesToDo - action.size
    updateArchive(archive, action, index, stream, remainingBytes)
      .map((_, remainingBytes))
  }

  private def updateArchive(
      archive: ThorpArchive,
      action: Action,
      index: Int,
      stream: Stream[StorageQueueEvent],
      remainingBytes: Long
  ) =
    for {
      event <- archive.update(index, action, remainingBytes)
    } yield stream ++ Stream(event)

}

object Program extends Program
