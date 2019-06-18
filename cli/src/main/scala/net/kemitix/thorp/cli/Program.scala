package net.kemitix.thorp.cli

import cats.Monad
import cats.effect.ExitCode
import cats.implicits._
import net.kemitix.thorp.aws.lib.S3ClientBuilder
import net.kemitix.thorp.core.{ConfigOption, ConfigValidation, ConfigurationBuilder, Sync}
import net.kemitix.thorp.domain.Logger

object Program {

  def reportErrors[M[_]: Monad](errors: List[ConfigValidation]): M[ExitCode] = {
    implicit val logger: Logger[M] = new PrintLogger[M]()
    for {
      _ <- logger.error("There were errors:")
      _ <- Monad[M].pure(errors.map(cv => logger.error(s" - ${cv.errorMessage}")))
    } yield ExitCode.Error
  }

  def apply[M[_]: Monad](configOptions: Seq[ConfigOption]): M[ExitCode] =
    ConfigurationBuilder(configOptions) match {
      case Left(errors) => reportErrors[M](errors.toList)
      case Right(config) =>
        implicit val logger: Logger[M] = new PrintLogger[M](config.debug)
        for {
          _ <- Sync.run[M](config, S3ClientBuilder.defaultClient)
        } yield ExitCode.Success
    }

}
