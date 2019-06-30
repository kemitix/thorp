package net.kemitix.thorp.cli

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val exitCaseLogger = new PrintLogger(false)
    ParseArgs(args)
      .map(Program(_))
      .getOrElse(IO(ExitCode.Error))
      .guaranteeCase {
          case Canceled => exitCaseLogger.warn("Interrupted")
          case Error(e) => exitCaseLogger.error(e.getMessage)
          case Completed => IO.unit
      }
  }

}
