package net.kemitix.s3thorp.cli

import com.typesafe.scalalogging.LazyLogging
import net.kemitix.s3thorp.domain.Config

class Logger extends LazyLogging {

  def info(level: Int, message: String)(implicit config: Config): Unit = if (config.verbose >= level) logger.info(s"1:$message")

  def warn(message: String): Unit = logger.warn(message)

  def error(message: String): Unit = logger.error(message)

}
