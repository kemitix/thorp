package net.kemitix.thorp.storage.aws

import cats.effect.IO
import net.kemitix.thorp.domain.Logger

trait ListerLogger {
  def logFetchBatch(implicit l: Logger): IO[Unit] = l.info("Fetching remote summaries...")
}
object ListerLogger extends ListerLogger
