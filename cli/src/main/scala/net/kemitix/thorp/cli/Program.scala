package net.kemitix.thorp.cli

import cats.effect.{ExitCode, IO}
import net.kemitix.thorp.core.{ConfigOption, Sync}
import net.kemitix.thorp.domain.Logger
import net.kemitix.thorp.storage.aws.S3StorageServiceBuilder.defaultStorageService

trait Program {

  def apply(cliOptions: Seq[ConfigOption]): IO[ExitCode] = {
    implicit val logger: Logger = new PrintLogger()
    Sync(defaultStorageService)(cliOptions) flatMap {
      case Left(errors) =>
        for {
          _ <- logger.error(s"There were errors:")
          _ <- IO.pure(errors.map(error => logger.error(s" - $error")))
        } yield ExitCode.Error
      case Right(_) => IO.pure(ExitCode.Success)
    }
  }

}

object Program extends Program