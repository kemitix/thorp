package net.kemitix.thorp.cli

import cats.effect.{ExitCode, IO}
import cats.implicits._
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.{Logger, StorageQueueEvent}
import net.kemitix.thorp.storage.aws.S3StorageServiceBuilder.defaultStorageService
import net.kemitix.thorp.storage.aws.S3HashService.defaultHashService

trait Program {

  def apply(cliOptions: Seq[ConfigOption]): IO[ExitCode] = {
    implicit val logger: Logger = new PrintLogger()
    for {
      actions <- Synchronise(defaultStorageService, defaultHashService, cliOptions).valueOrF(handleErrors)
      events <- handleActions(UnversionedMirrorArchive.default(defaultStorageService), actions)
      _ <- SyncLogging.logRunFinished(events)
    } yield ExitCode.Success
  }

  private def handleErrors(implicit logger: Logger): List[String] => IO[Stream[Action]] = {
    errors => {
      for {
        _ <- logger.error("There were errors:")
        _ <- errors.map(error => logger.error(s" - $error")).sequence
      } yield Stream()
    }
  }

  private def handleActions(archive: ThorpArchive,
                            actions: Stream[Action]): IO[Stream[StorageQueueEvent]] =
    actions.foldRight(Stream[IO[StorageQueueEvent]]()) {
      (action, stream) => archive.update(action) ++ stream
    }.sequence
}

object Program extends Program
