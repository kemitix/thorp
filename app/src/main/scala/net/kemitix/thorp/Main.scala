package net.kemitix.thorp

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.filesystem.FileSystem
import net.kemitix.thorp.storage.aws.S3Storage
import net.kemitix.thorp.storage.aws.hasher.S3Hasher
import zio.{App, ZIO}

object Main extends App {

  object LiveThorpApp
      extends S3Storage.Live
      with Console.Live
      with Config.Live
      with FileSystem.Live
      with S3Hasher.Live

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    Program
      .run(args)
      .provide(LiveThorpApp)
      .fold(_ => 1, _ => 0)

}