package net.kemitix.thorp

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.filesystem.FileSystem
import net.kemitix.thorp.lib.FileScanner
import net.kemitix.thorp.storage.aws.S3Storage
import net.kemitix.thorp.storage.aws.hasher.S3Hasher
import zio.clock.Clock
import zio.{App, ZEnv, ZIO}

object Main extends App {

  object LiveThorpApp
      extends S3Storage.Live
      with Console.Live
      with Clock.Live
      with Config.Live
      with FileSystem.Live
      with S3Hasher.Live
      with FileScanner.Live

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    Program
      .run(args)
      .provide(LiveThorpApp)
      .fold(_ => 1, _ => 0)

}
