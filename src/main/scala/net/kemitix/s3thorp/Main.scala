package net.kemitix.s3thorp

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  def parseArgs(args: List[String]): IO[Config] = {
    import scopt.OParser
    val builder = OParser.builder[Config]
    val configParser: OParser[Unit, Config] = {
      import builder._
      OParser.sequence(
        programName("S3Thorp"),
        head("s3thorp", "0.1.0"),
        opt[String]('s', "source")
          .action((str, c) => c.copy(source = str)),
        opt[String]('b', "bucket")
          .action((str, c) => c.copy(bucket = str))
          .text("S3 bucket name"),
        opt[String]('p', "prefix")
          .action((str, c) => c.copy(prefix = str))
          .text("Prefix within the S3 Bucket")
      )
    }
    val defaultConfig = Config("def-bucket", "def-prefix", "def-source")
    OParser.parse(configParser, args, defaultConfig) match {
      case Some(config) => IO.pure(config)
      case _ => IO.raiseError(new IllegalArgumentException)
    }
  }

  def putStrLn(value: String) = IO { println(value) }

  def sync(c: Config): IO[Unit] =
    for {
      _ <- putStrLn("S3Thorp - hashed sync for s3")
      _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    } yield ()

  def program(args: List[String]): IO[ExitCode] =
    for {
      a <- parseArgs(args)
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
