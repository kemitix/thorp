package net.kemitix.thorp.cli

import net.kemitix.thorp.console._
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.{StorageQueueEvent, SyncTotals}
import net.kemitix.thorp.storage.api.Storage
import net.kemitix.thorp.storage.aws.S3HashService.defaultHashService
import net.kemitix.thorp.storage.aws.S3Storage
import zio.{Task, TaskR, ZIO}

trait Program {

  type Program[A] = ZIO[Console, Throwable, Unit]

  lazy val version = s"Thorp v${thorp.BuildInfo.version}"

  def run(args: List[String]): Program[Unit] = {
    def showVersion(cli: ConfigOptions) = ConfigQuery.showVersion(cli)
    for {
      cli <- CliArgs.parse(args)
      _   <- ZIO.when(showVersion(cli))(putStrLn(version))
      _   <- ZIO.when(!showVersion(cli))(execute(cli).catchAll(handleErrors))
    } yield ()
  }

  private def execute(
      cliOptions: ConfigOptions): ZIO[Console, Throwable, Unit] = {
    val storage = S3Storage.Live.storage
    for {
      plan    <- PlanBuilder.createPlan(storage, defaultHashService, cliOptions)
      archive <- thorpArchive(cliOptions, plan.syncTotals, storage)
      events  <- handleActions(archive, plan)
      _       <- storage.shutdown
      _       <- SyncLogging.logRunFinished(events)
    } yield ()
  }

  private def handleErrors(throwable: Throwable): ZIO[Console, Nothing, Unit] =
    for {
      _ <- putStrLn("There were errors:")
      _ <- throwable match {
        case ConfigValidationException(errors) =>
          ZIO.foreach(errors)(error => putStrLn(s"- $error"))
        case x => throw x
      }
    } yield ()

  def thorpArchive(
      cliOptions: ConfigOptions,
      syncTotals: SyncTotals,
      storage: Storage.Service
  ): Task[ThorpArchive] = Task {
    UnversionedMirrorArchive.default(
      storage,
      ConfigQuery.batchMode(cliOptions),
      syncTotals
    )
  }

  private def handleActions(
      archive: ThorpArchive,
      syncPlan: SyncPlan
  ): TaskR[Console, Stream[StorageQueueEvent]] = {
    type Accumulator = (Stream[StorageQueueEvent], Long)
    val zero: Accumulator = (Stream(), syncPlan.syncTotals.totalSizeBytes)
    TaskR
      .foldLeft(syncPlan.actions.zipWithIndex)(zero)((acc, indexedAction) => {
        val (action, index)     = indexedAction
        val (stream, bytesToDo) = acc
        val remainingBytes      = bytesToDo - action.size
        (for {
          event <- archive.update(index, action, remainingBytes)
          events = stream ++ Stream(event)
        } yield events)
          .map((_, remainingBytes))
      })
      .map {
        case (events, _) => events
      }
  }

}

object Program extends Program
