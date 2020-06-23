package net.kemitix.thorp

import net.kemitix.thorp.lib.FileScanner
import zio.clock.Clock
import zio.{App, ZEnv, ZIO}

object Main extends App {

  object LiveThorpApp extends Clock.Live with FileScanner.Live

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    Program
      .run(args)
      .provide(LiveThorpApp)
      .fold(_ => 1, _ => 0)

}
