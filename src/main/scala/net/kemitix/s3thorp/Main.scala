package net.kemitix.s3thorp

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  def parseArgs(args: List[String]): IO[Config] = IO.pure(Config("", ""))

  def program(args: List[String]): IO[ExitCode] = for {
    a <- parseArgs(args)
    _ <- S3Thorp(a)
  } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] =
    program(args)
      .guaranteeCase {
        case Canceled => IO(println("Interrupted"))
        case Error(e) => IO(println("ERROR: " + e.getMessage))
        case Completed => IO(println("Done"))
      }

}
