package net.kemitix.s3thorp

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  def putStrLn(value: String) = IO { println(value) }

  def sync(c: Config): IO[Unit] =
    for {
      _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    } yield ()

  def program(args: List[String]): IO[ExitCode] =
    for {
      _ <- putStrLn("S3Thorp - hashed sync for s3")
      a <- ParseArgs(args)
      _ <- sync(a)
    } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] =
    program(args)
      .guaranteeCase {
        case Canceled => IO(println("Interrupted"))
        case Error(e) => IO(println("ERROR: " + e.getMessage))
        case Completed => IO(println("Done"))
      }

}
