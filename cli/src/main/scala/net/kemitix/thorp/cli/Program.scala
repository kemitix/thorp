package net.kemitix.thorp.cli

import cats.effect.{ExitCode, IO}
import cats.implicits._
import net.kemitix.thorp.core.{Action, ActionSubmitter, ConfigOption, Synchronise}
import net.kemitix.thorp.domain.{Logger, StorageQueueEvent}
import net.kemitix.thorp.storage.aws.S3StorageServiceBuilder.defaultStorageService

trait Program {

  def apply(cliOptions: Seq[ConfigOption]): IO[ExitCode] = {
    implicit val logger: Logger = new PrintLogger()
    Synchronise(defaultStorageService, cliOptions).flatMap {
      case Left(errors) =>
        for {
          _ <- logger.error(s"There were errors:")
          _ <- errors.map(error => logger.error(s" - $error")).sequence
        } yield ExitCode.Error
      case Right(actions) =>
        for {
          _ <- handleActions(actions)
        } yield ExitCode.Success
    }
  }

  private def handleActions(actions: Stream[Action])
                           (implicit l: Logger): IO[Stream[StorageQueueEvent]] = {
    actions.foldRight(Stream[IO[StorageQueueEvent]]()) {
      (action, stream) => ActionSubmitter.submitAction(defaultStorageService, action) ++ stream
    }.sequence
  }
}

object Program extends Program