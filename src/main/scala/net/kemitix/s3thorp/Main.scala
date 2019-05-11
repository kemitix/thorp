package net.kemitix.s3thorp

import java.nio.file.Paths

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import net.kemitix.s3thorp.awssdk.S3Client

object Main extends IOApp with Logging {

  val defaultConfig: Config =
    Config("(none)", "", Paths.get(".").toFile)

  val sync = new Sync(S3Client.defaultClient)

  def program(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO(logger.info("S3Thorp - hashed sync for s3"))
      a <- ParseArgs(args, defaultConfig)
      _ <- sync.run(a)
    } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] =
    program(args)
      .guaranteeCase {
        case Canceled => IO(logger.warn("Interrupted"))
        case Error(e) => IO(logger.error(e.getMessage))
        case Completed => IO(logger.info("Done"))
      }

}
