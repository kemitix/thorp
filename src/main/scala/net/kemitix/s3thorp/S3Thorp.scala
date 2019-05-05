package net.kemitix.s3thorp

import cats.effect.IO

object S3Thorp {

  def putStrLn(value: String): IO[Unit] = IO(println(value))

  def apply(args: List[String]): IO[Unit] = {
    for {
      _ <- putStrLn("S3Thorp - hashed sync for s3")
    } yield ()
  }

}
