package net.kemitix.thorp

import zio.ZIO

package object console extends MyConsole.Service {

  final val consoleService: ZIO[MyConsole, Nothing, MyConsole.Service] =
    ZIO.access(_.console)

  final def putStrLn(line: String): ZIO[MyConsole, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

  override def putStrLn(line: ConsoleOut): ZIO[MyConsole, Nothing, Unit] =
    putStrLn(line.en)

}
