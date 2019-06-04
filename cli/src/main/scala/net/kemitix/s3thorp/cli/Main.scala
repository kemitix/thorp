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

  val md5HashGenerator: File => MD5Hash = file => new MD5HashGenerator {}.md5File(file)(defaultConfig)

  val sync = new Sync(S3ClientBuilder.defaultClient, md5HashGenerator)

  val logger = new Logger

  def program(args: List[String]): IO[ExitCode] =
    for {
      config <- ParseArgs(args, defaultConfig)
      _ <- IO(logger.info(1, "S3Thorp - hashed sync for s3")(config))
      _ <- sync.run(
        i => logger.info(1, i)(config),
        w => logger.warn(w),
        e => logger.error(e))(config)
    } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] =
    program(args)
      .guaranteeCase {
        case Canceled => IO(logger.warn("Interrupted"))
        case Error(e) => IO(logger.error(e.getMessage))
        case Completed => IO(logger.info(1, "Done")(defaultConfig))
      }

}
