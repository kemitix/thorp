package net.kemitix.thorp.cli

import net.kemitix.thorp.console.Console
import zio.internal.PlatformLive
import zio.{App, Runtime, UIO, ZIO}

object Main extends App {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    runtime.unsafeRun {
      appLogic(args).fold(_ => UIO(1), _ => UIO(0))
    }

  private def appLogic(args: List[String]): ZIO[Console, Throwable, Unit] =
    for {
      cliOptions <- ParseArgs(args)
      _          <- Program.run(cliOptions)
    } yield ()

}
