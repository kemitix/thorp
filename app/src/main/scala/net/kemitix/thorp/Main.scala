package net.kemitix.thorp

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.lib.FileScanner
import net.kemitix.thorp.storage.aws.S3Storage
import zio.clock.Clock
import zio.{App, ZEnv, ZIO}

object Main extends App {

  object LiveThorpApp
      extends S3Storage.Live
      with Console.Live
      with Clock.Live
      with FileScanner.Live

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    Program
      .run(args)
      .provide(LiveThorpApp)
      .fold(_ => 1, _ => 0)

}
