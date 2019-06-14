package net.kemitix.s3thorp.cli

import java.io.File

import cats.Monad
import cats.implicits._
import cats.effect.ExitCode
import net.kemitix.s3thorp.aws.lib.S3ClientBuilder
import net.kemitix.s3thorp.core.MD5HashGenerator.md5File
import net.kemitix.s3thorp.core.{MD5HashGenerator, Sync}
import net.kemitix.s3thorp.domain.Config

object Program {

  def apply[M[_]: Monad](config: Config): M[ExitCode] = {
    val logger = new Logger[M](config.verbose)
    val info = (_: Int) => (m: String) => logger.info(m)
    val warn = (w: String) => logger.warn(w)
    for {
      _ <- info(1)("S3Thorp - hashed sync for s3")
      _ <- Sync.run[M](config, S3ClientBuilder.defaultClient, hashGenerator(info), info, warn)
    } yield ExitCode.Success
  }

  private def hashGenerator[M[_]: Monad](info: Int => String => M[Unit]) = {
    implicit val logInfo: Int => String => M[Unit] = info
    file: File => md5File[M](file)
  }

}
