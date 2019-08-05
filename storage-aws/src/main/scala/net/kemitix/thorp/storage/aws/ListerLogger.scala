package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.console._
import zio.RIO

trait ListerLogger {
  def logFetchBatch: RIO[Console, Unit] =
    Console.putStrLn("Fetching remote summaries...")
}
object ListerLogger extends ListerLogger
