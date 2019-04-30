package net.kemitix.s3thorp

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      ec <- IO(println("S3Thorp - hashed sync for s3")).as(ExitCode.Success)
    } yield ec).guaranteeCase {
        case Canceled => IO(println("Interrupted"))
        case Error(e) => IO(println("ERROR: " + e))
        case Completed => IO(println("Done"))
      }

}
