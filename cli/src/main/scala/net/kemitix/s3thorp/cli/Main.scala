package net.kemitix.s3thorp.cli

import java.io.File
import java.nio.file.Paths

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import net.kemitix.s3thorp.aws.lib.S3ClientBuilder
import net.kemitix.s3thorp.domain.{Config, MD5Hash}
import net.kemitix.s3thorp.{MD5HashGenerator, Sync}

object Main extends IOApp {

  val defaultConfig: Config =
    Config(source = Paths.get(".").toFile)

  def program(args: List[String]): IO[ExitCode] =
    for {
      config <- ParseArgs(args, defaultConfig)
      logger = new Logger(config.verbose)
      info = (l: Int) => (m: String) => logger.info(l)(m)
      md5HashGenerator = (file: File) => new MD5HashGenerator {}.md5File(file)(info)
      _ <- IO(logger.info(1)("S3Thorp - hashed sync for s3"))
      _ <- Sync.run(
        S3ClientBuilder.defaultClient,
        md5HashGenerator,
        l => i => logger.info(l)(i),
        w => logger.warn(w),
        e => logger.error(e))(config)
    } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = new Logger(1)
    program(args)
      .guaranteeCase {
        case Canceled => IO(logger.warn("Interrupted"))
        case Error(e) => IO(logger.error(e.getMessage))
        case Completed => IO(logger.info(1)("Done"))
      }
  }

}
