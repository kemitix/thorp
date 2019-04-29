package net.kemitix.s3thorp

import cats.effect.ExitCase.Canceled
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._

object Main extends IOApp {

  def exec(args: List[String]): IO[ExitCode] =
    for {
      ec <- IO(println("S3Thorp - hashed sync for s3")).as(ExitCode.Success)
    } yield ec

  override def run(args: List[String]): IO[ExitCode] =
    exec(args).guaranteeCase {
      case Canceled => IO(println("Interrupted"))
      case _ => IO(println("Done"))
    }

}
