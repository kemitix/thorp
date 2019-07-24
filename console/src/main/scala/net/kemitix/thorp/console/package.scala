package net.kemitix.thorp

import zio.ZIO

package object console extends Console.Service {

  final val consoleService: ZIO[Console, Nothing, Console.Service] =
    ZIO.access(_.console)

  final def putStrLn(line: String): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

  override def putStrLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
    putStrLn(line.en)

}
