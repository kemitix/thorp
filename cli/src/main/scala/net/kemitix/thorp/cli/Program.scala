package net.kemitix.thorp.cli

import cats.effect.{ExitCode, IO}
import cats.implicits._
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.{Logger, StorageQueueEvent}
import net.kemitix.thorp.storage.aws.S3HashService.defaultHashService
import net.kemitix.thorp.storage.aws.S3StorageServiceBuilder.defaultStorageService

trait Program {

  def run(cliOptions: ConfigOptions): IO[ExitCode] = {
    implicit val logger: Logger = new PrintLogger()
    if (ConfigQuery.showVersion(cliOptions)) IO {
        println(s"Thorp v${thorp.BuildInfo.version}")
        ExitCode.Success
    } else {
      for {
        actions <- Synchronise.createPlan(defaultStorageService, defaultHashService, cliOptions).valueOrF(handleErrors)
        events <- handleActions(UnversionedMirrorArchive.default(defaultStorageService, ConfigQuery.batchMode(cliOptions)), actions)
        _ <- defaultStorageService.shutdown
        _ <- SyncLogging.logRunFinished(events)
      } yield ExitCode.Success
    }
  }

  private def handleErrors(implicit logger: Logger): List[String] => IO[SyncPlan] = {
    errors => {
      for {
        _ <- logger.error("There were errors:")
        _ <- errors.map(error => logger.error(s" - $error")).sequence
      } yield SyncPlan()
    }
  }

  private def handleActions(archive: ThorpArchive,
                            actions: SyncPlan)
                           (implicit l: Logger): IO[Stream[StorageQueueEvent]] =
    actions.actions.foldLeft(Stream[IO[StorageQueueEvent]]()) {
      (stream, action) => archive.update(action) ++ stream
    }.sequence
}

object Program extends Program
