package net.kemitix.s3thorp.cli

import com.typesafe.scalalogging.LazyLogging
import net.kemitix.s3thorp.domain.Config

class Logger(verbosity: Int) extends LazyLogging {

  def info(level: Int)(message: String): Unit = if (verbosity >= level) logger.info(s"1:$message")

  def warn(message: String): Unit = logger.warn(message)

  def error(message: String): Unit = logger.error(message)

}
