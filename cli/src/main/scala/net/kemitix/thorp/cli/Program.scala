package net.kemitix.thorp.cli

import cats.effect.{ExitCode, IO}
import cats.implicits._
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.{Logger, StorageQueueEvent}
import net.kemitix.thorp.storage.aws.S3HashService.defaultHashService
import net.kemitix.thorp.storage.aws.S3StorageServiceBuilder.defaultStorageService

trait Program extends PlanBuilder {

  def run(cliOptions: ConfigOptions): IO[ExitCode] = {
    implicit val logger: Logger = new PrintLogger()
    if (ConfigQuery.showVersion(cliOptions))
      for {
        _ <- logger.info(s"Thorp v${thorp.BuildInfo.version}")
      } yield ExitCode.Success
    else
      for {
        syncPlan <- createPlan(
          defaultStorageService,
          defaultHashService,
          cliOptions
        ).valueOrF(handleErrors)
        archive <- thorpArchive(cliOptions, syncPlan)
        events  <- handleActions(archive, syncPlan)
        _       <- defaultStorageService.shutdown
        _       <- SyncLogging.logRunFinished(events)
      } yield ExitCode.Success
  }

  def thorpArchive(
      cliOptions: ConfigOptions,
      syncPlan: SyncPlan
  ): IO[ThorpArchive] =
    IO.pure(
      UnversionedMirrorArchive.default(
        defaultStorageService,
        ConfigQuery.batchMode(cliOptions),
        syncPlan.syncTotals
      ))

  private def handleErrors(
      implicit logger: Logger
  ): List[String] => IO[SyncPlan] = errors => {
    for {
      _ <- logger.error("There were errors:")
      _ <- errors.map(error => logger.error(s" - $error")).sequence
    } yield SyncPlan()
  }

  private def handleActions(
      archive: ThorpArchive,
      syncPlan: SyncPlan
  )(implicit l: Logger): IO[Stream[StorageQueueEvent]] = {
    type Accumulator = (Stream[IO[StorageQueueEvent]], Long)
    val zero: Accumulator = (Stream(), syncPlan.syncTotals.totalSizeBytes)
    val (actions, _) = syncPlan.actions.zipWithIndex.reverse
      .foldLeft(zero) { (acc: Accumulator, indexedAction) =>
        {
          val (stream, bytesToDo) = acc
          val (action, index)     = indexedAction
          val remainingBytes      = bytesToDo - action.size
          (
            archive.update(index, action, remainingBytes) ++ stream,
            remainingBytes
          )
        }
      }
    actions.sequence
  }
}

object Program extends Program
