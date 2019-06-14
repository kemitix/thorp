package net.kemitix.s3thorp.cli

import java.io.File
import java.nio.file.Paths

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import net.kemitix.s3thorp.core.MD5HashGenerator.md5File
import net.kemitix.s3thorp.aws.lib.S3ClientBuilder
import net.kemitix.s3thorp.core.Sync
import net.kemitix.s3thorp.domain.Config

object Main extends IOApp {

  val defaultConfig: Config =
    Config(source = Paths.get(".").toFile)

  def program(config: Config): IO[ExitCode] = {
    val logger = new Logger[IO] (config.verbose)
    val info = (l: Int) => (m: String) => logger.info(l) (m)
    val warn = (w: String) => logger.warn(w)
    for {
      _ <- logger.info(1)("S3Thorp - hashed sync for s3")
      _ <- Sync.run[IO](config, S3ClientBuilder.defaultClient, hashGenerator(info), info, warn)
    } yield ExitCode.Success
  }

  private def hashGenerator(info: Int => String => IO[Unit]) = {
    implicit val logInfo: Int => String => IO[Unit] = info
    file: File => md5File[IO](file)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = new Logger[IO](1)
    ParseArgs(args, defaultConfig)
      .map(program)
      .getOrElse(IO(ExitCode.Error))
      .guaranteeCase {
          case Canceled => logger.warn("Interrupted")
          case Error(e) => logger.error(e.getMessage)
          case Completed => logger.info(1)("Done")
      }
  }

}
