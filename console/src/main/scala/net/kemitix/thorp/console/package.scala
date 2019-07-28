package net.kemitix.thorp

import zio.ZIO

package object console {

  final val consoleService: ZIO[Console, Nothing, Console.Service] =
    ZIO.access(_.console)

  final def putStrLn(line: String): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

  final def putStrLn(line: ConsoleOut): ZIO[Console, Nothing, Unit] =
    ZIO.accessM(_.console putStrLn line)

}
