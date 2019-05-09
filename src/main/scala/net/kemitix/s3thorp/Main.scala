package net.kemitix.s3thorp

import java.nio.file.Paths

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{ExitCode, IO, IOApp}
import net.kemitix.s3thorp.awssdk.S3Client

object Main extends IOApp {

  def putStrLn(value: String) = IO { println(value) }

  val defaultConfig: Config =
    Config("(none)", "", Paths.get(".").toFile)

  val sync = new Sync(S3Client.defaultClient)

  def program(args: List[String]): IO[ExitCode] =
    for {
      _ <- putStrLn("S3Thorp - hashed sync for s3")
      a <- ParseArgs(args, defaultConfig)
      _ <- sync.run(a)
    } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] =
    program(args)
      .guaranteeCase {
        case Canceled => IO(println("Interrupted"))
        case Error(e) => IO(println("ERROR: " + e.getMessage))
        case Completed => IO(println("Done"))
      }

}
