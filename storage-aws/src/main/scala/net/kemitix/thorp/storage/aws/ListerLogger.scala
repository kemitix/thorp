package net.kemitix.thorp.storage.aws

import zio.TaskR
import zio.console._

trait ListerLogger {
  def logFetchBatch: TaskR[Console, Unit] =
    putStrLn("Fetching remote summaries...")
}
object ListerLogger extends ListerLogger
