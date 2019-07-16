package net.kemitix.thorp.storage.aws

import cats.effect.IO
import net.kemitix.thorp.domain.Logger

class DummyLogger extends Logger {

  override def debug(message: => String): IO[Unit] = IO.unit

  override def info(message: => String): IO[Unit] = IO.unit

  override def warn(message: String): IO[Unit] = IO.unit

  override def error(message: String): IO[Unit] = IO.unit

  override def withDebug(debug: Boolean): Logger = this

}
