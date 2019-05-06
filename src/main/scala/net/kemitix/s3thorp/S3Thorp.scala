package net.kemitix.s3thorp

import cats.effect.IO

object S3Thorp {

  def putStrLn(value: String) = IO { println(value) }

  def apply(args: Config): IO[Unit] = {
    for {
      _ <- putStrLn("S3Thorp - hashed sync for s3")
    } yield ()
  }

}
