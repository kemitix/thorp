package net.kemitix.thorp.console

import java.io.PrintStream

import zio.{IO, ZIO}

trait MyConsole {
  val console: MyConsole.Service
}

object MyConsole {

  trait Service {
    def putStrLn(line: ConsoleOut): ZIO[MyConsole, Nothing, Unit]
    def putStrLn(line: String): ZIO[MyConsole, Nothing, Unit]
  }

  trait Live extends MyConsole {
    val console: Service = new Service {
      override def putStrLn(line: ConsoleOut): ZIO[MyConsole, Nothing, Unit] =
        putStrLn(line)
      override def putStrLn(line: String): ZIO[MyConsole, Nothing, Unit] =
        putStrLn(Console.out)(line)
      final def putStrLn(stream: PrintStream)(
          line: String): ZIO[MyConsole, Nothing, Unit] =
        IO.effectTotal(Console.withOut(stream) {
          Console.println(line)
        })
    }
  }

  object Live extends Live
}
