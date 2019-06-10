package net.kemitix.s3thorp.cli

import cats.effect.IO

class Logger(verbosity: Int)  {

  def info(level: Int)(message: String): IO[Unit] =
    if (verbosity >= level) IO(println(s"[INFO:$level] $message"))
    else IO.unit

  def warn(message: String): IO[Unit] = IO(println(s"[  WARN] $message"))

  def error(message: String): IO[Unit] = IO(println(s"[ ERROR] $message"))

}
