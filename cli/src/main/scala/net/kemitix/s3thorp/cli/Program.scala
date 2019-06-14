package net.kemitix.s3thorp.cli

import java.io.File

import cats.Monad
import cats.effect.ExitCode
import cats.implicits._
import net.kemitix.s3thorp.aws.lib.S3ClientBuilder
import net.kemitix.s3thorp.core.MD5HashGenerator.md5File
import net.kemitix.s3thorp.core.Sync
import net.kemitix.s3thorp.domain.Config

object Program {

  def apply[M[_]: Monad](config: Config): M[ExitCode] = {
    val logger = new PrintLogger[M](config.verbose)
    val debug = (m: String) => logger.debug(m)
    val info = (_: Int) => (m: String) => logger.info(m)
    val warn = (w: String) => logger.warn(w)
    for {
      _ <- logger.info("S3Thorp - hashed sync for s3")
      _ <- Sync.run[M](config, S3ClientBuilder.defaultClient, hashGenerator(debug), info, warn)
    } yield ExitCode.Success
  }

  private def hashGenerator[M[_]: Monad](debug: String => M[Unit]) = {
    implicit val logDebug: String => M[Unit] = debug
    file: File => md5File[M](file)
  }

}
