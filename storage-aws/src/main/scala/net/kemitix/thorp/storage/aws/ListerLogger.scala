package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.console._
import zio.TaskR

trait ListerLogger {
  def logFetchBatch: TaskR[Console, Unit] =
    putStrLn("Fetching remote summaries...")
}
object ListerLogger extends ListerLogger
