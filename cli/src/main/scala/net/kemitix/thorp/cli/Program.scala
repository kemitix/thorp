package net.kemitix.thorp.cli

import cats.effect.{ExitCode, IO}
import net.kemitix.thorp.aws.lib.S3StorageServiceBuilder
import net.kemitix.thorp.core.{ConfigOption, Sync}
import net.kemitix.thorp.domain.Logger

trait Program {

  def apply(configOptions: Seq[ConfigOption]): IO[ExitCode] = {
    implicit val logger: Logger = new PrintLogger()
    Sync(S3StorageServiceBuilder.defaultStorageService)(configOptions) flatMap {
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