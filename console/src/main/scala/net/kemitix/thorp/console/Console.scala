package net.kemitix.thorp.console

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference

import net.kemitix.thorp.config.Config
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

  final val consoleService: ZIO[Console, Nothing, Console.Service] =
    ZIO.access(_.console)

  final def putStrLn(line: String): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

  final def putMessageLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

  final def putMessageLnB(
      line: ConsoleOut.WithBatchMode): ZIO[Console with Config, Nothing, Unit] =
    ZIO.accessM(line() >>= _.console.putStrLn)

}
