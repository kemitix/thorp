package net.kemitix.thorp.cli

import net.kemitix.thorp.console.Console
import zio.{App, ZIO}

object Main extends App {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    appLogic(args)
      .provide(Console.Live)
      .fold(_ => 1, _ => 0)

  private def appLogic(args: List[String]): ZIO[Console, Throwable, Unit] =
    for {
      cliOptions <- ParseArgs(args)
      _          <- Program.run(cliOptions)
    } yield ()

}
