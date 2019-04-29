package net.kemitix.s3thorp

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    IO(println("S3Thorp - hashed sync for s3")).as(ExitCode.Success)

}
