package net.kemitix.s3thorp.cli

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging

class Logger(verbosity: Int) extends LazyLogging {

  def info(level: Int)(message: String): IO[Unit] =
    if (verbosity >= level) IO(logger.info(s"1:$message"))
    else IO.unit

  def warn(message: String): IO[Unit] = IO(logger.warn(message))

  def error(message: String): IO[Unit] = IO(logger.error(message))

}
