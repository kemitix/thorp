package net.kemitix.thorp.cli

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.storage.aws.S3Storage
import zio.{App, ZIO}

object Main extends App {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    Program
      .run(args)
      .provide(new S3Storage.Live with Console.Live)
      .fold(_ => 1, _ => 0)

}
