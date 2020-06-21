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
    def putMessageLn(line: ConsoleOut): ZIO[Console, Nothing, Unit]
    def putStrLn(line: String): ZIO[Console, Nothing, Unit]
    def putStr(line: String): ZIO[Console, Nothing, Unit]
  }

  trait Live extends Console {
    val console: Service = new Service {
      override def putMessageLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
        putStrLn(line.en())
      override def putStrLn(line: String): ZIO[Console, Nothing, Unit] =
        putStrLnPrintStream(SConsole.out)(line)
      override def putStr(line: String): ZIO[Console, Nothing, Unit] =
        putStrPrintStream(SConsole.out)(line)
      final def putStrLnPrintStream(
        stream: PrintStream
      )(line: String): ZIO[Console, Nothing, Unit] =
        UIO(SConsole.withOut(stream)(SConsole.println(line)))
      final def putStrPrintStream(
        stream: PrintStream
      )(line: String): ZIO[Console, Nothing, Unit] =
        UIO(SConsole.withOut(stream)(SConsole.print(line)))

    }
  }

  object Live extends Live

  trait Test extends Console {

    private val output = new AtomicReference(List.empty[String])
    def getOutput: List[String] = output.get

    val console: Service = new Service {
      override def putMessageLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
        putStrLn(line.en)

      override def putStrLn(line: String): ZIO[Console, Nothing, Unit] = {
        val _ = output.accumulateAndGet(List(line), (a, b) => a ++ b)
        ZIO.succeed(())
      }

      override def putStr(line: String): ZIO[Console, Nothing, Unit] = {
        val _ = output.accumulateAndGet(List(line), (a, b) => a ++ b)
        ZIO.succeed(())
      }
    }
  }

  object Test extends Test

  final val consoleService: ZIO[Console, Nothing, Console.Service] =
    ZIO.access(_.console)

  final def putStrLn(line: String): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

  final def putStr(line: String): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console.putStr(line))

  final def putMessageLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putMessageLn line)

  final def putMessageLnB(line: ConsoleOut.WithBatchMode,
                          batchMode: Boolean): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(UIO(line(batchMode)) >>= _.console.putStrLn)

}
