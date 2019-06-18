package net.kemitix.thorp.cli

import cats.Monad
import cats.effect.ExitCode
import cats.implicits._
import net.kemitix.thorp.aws.lib.S3ClientBuilder
import net.kemitix.thorp.core.{ConfigOption, Sync}
import net.kemitix.thorp.domain.Logger

trait Program {

  def apply[M[_]: Monad](configOptions: Seq[ConfigOption]): M[ExitCode] = {
    implicit val logger: Logger[M] = new PrintLogger[M]()
    Sync[M](S3ClientBuilder.defaultClient[M])(configOptions) flatMap {
      case Left(errors) =>
        for {
          _ <- logger.error(s"There were errors:")
          _ <- Monad[M].pure(errors.map(error => logger.error(s" - $error")))
        } yield ExitCode.Error
      case Right(_) => Monad[M].pure(ExitCode.Success)
    }
  }

}

object Program extends Program