package net.kemitix.thorp.cli

import zio.{App, ZIO}

object Main extends App {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    for {
      cliOptions <- ParseArgs(args)
      _          <- Program.run(cliOptions)
    } yield ()
  }.fold(failure => 1, success => 0)

}
