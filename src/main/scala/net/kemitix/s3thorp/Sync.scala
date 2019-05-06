package net.kemitix.s3thorp

import cats._
import cats.effect._
import Main.putStrLn

object Sync {
  def apply(c: Config): IO[Unit] = for {
    _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
  } yield ()

}
