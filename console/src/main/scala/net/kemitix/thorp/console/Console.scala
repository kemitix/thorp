package net.kemitix.thorp.console

import java.io.PrintStream
import scala.{Console => SConsole}

import zio.{UIO, ZIO}

trait Console {
  val console: Console.Service
}

object Console {

  trait Service {
    def putStrLn(line: ConsoleOut): ZIO[Console, Nothing, Unit]
    def putStrLn(line: String): ZIO[Console, Nothing, Unit]
  }

  trait Live extends Console {
    val console: Service = new Service {
      override def putStrLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
        putStrLn(line.en)
      override def putStrLn(line: String): ZIO[Console, Nothing, Unit] =
        putStrLn(SConsole.out)(line)
      final def putStrLn(stream: PrintStream)(
          line: String): ZIO[Console, Nothing, Unit] =
        UIO(SConsole.withOut(stream)(Console.println(line)))
    }
  }

  object Live extends Live
}
