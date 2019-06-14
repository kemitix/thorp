package net.kemitix.s3thorp.cli

import java.nio.file.Paths

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import net.kemitix.s3thorp.domain.Config

object Main extends IOApp {

  val defaultConfig: Config =
    Config(source = Paths.get(".").toFile)

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = new Logger[IO](1)
    ParseArgs(args, defaultConfig)
      .map(Program[IO])
      .getOrElse(IO(ExitCode.Error))
      .guaranteeCase {
          case Canceled => logger.warn("Interrupted")
          case Error(e) => logger.error(e.getMessage)
          case Completed => logger.info(1)("Done")
      }
  }

}
