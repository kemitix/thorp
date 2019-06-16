package net.kemitix.thorp.cli

import java.nio.file.Paths

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import net.kemitix.s3thorp.domain.Config

object Main extends IOApp {

  val defaultConfig: Config =
    Config(source = Paths.get(".").toFile)

  override def run(args: List[String]): IO[ExitCode] = {
    val exitCaseLogger = new PrintLogger[IO](false)
    ParseArgs(args, defaultConfig)
      .map(Program[IO])
      .getOrElse(IO(ExitCode.Error))
      .guaranteeCase {
          case Canceled => exitCaseLogger.warn("Interrupted")
          case Error(e) => exitCaseLogger.error(e.getMessage)
          case Completed => exitCaseLogger.info("Done")
      }
  }

}
