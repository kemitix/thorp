package net.kemitix.thorp.console

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference

import zio.{UIO, ZIO}

import scala.{Console => SConsole}

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
        UIO(SConsole.withOut(stream)(SConsole.println(line)))
    }
  }

  object Live extends Live

  trait Test extends Console {

    private val output          = new AtomicReference(List.empty[String])
    def getOutput: List[String] = output.get

    val console: Service = new Service {
      override def putStrLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
        putStrLn(line.en)

      override def putStrLn(line: String): ZIO[Console, Nothing, Unit] = {
        output.accumulateAndGet(List(line), (a, b) => a ++ b)
        ZIO.succeed(())
      }

    }
  }

  object Test extends Test

}
