package net.kemitix.thorp.cli

import cats.effect.IO
import net.kemitix.thorp.domain.Logger

class PrintLogger(isDebug: Boolean = false) extends Logger {

  override def debug(message: => String): IO[Unit] =
    if (isDebug) IO(println(s"[ DEBUG] $message"))
    else IO.unit

  override def info(message: => String): IO[Unit] =
    IO(println(s"[  INFO] $message"))

  override def warn(message: String): IO[Unit] =
    IO(println(s"[  WARN] $message"))

  override def error(message: String): IO[Unit] =
    IO(println(s"[ ERROR] $message"))

  override def withDebug(debug: Boolean): Logger =
    if (isDebug == debug) this
    else new PrintLogger(debug)

}
