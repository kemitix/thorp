package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console._
import zio.ZIO

trait SyncLogging {

  def logFileScan: ZIO[Config with Console, Nothing, Unit] =
    for {
      sources <- Config.sources
      _ <- Console.putStrLn(
        s"Scanning local files: ${sources.paths.mkString(", ")}...")
    } yield ()

}

object SyncLogging extends SyncLogging
