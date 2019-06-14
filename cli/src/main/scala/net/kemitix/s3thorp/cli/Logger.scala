package net.kemitix.s3thorp.cli

import cats.Monad

class Logger[M[_]: Monad](verbosity: Int)  {

  def debug(message: => String): M[Unit] = Monad[M].pure(println(s"[ DEBUG] $message"))

  def info(level: Int)(message: String): M[Unit] =
    if (verbosity >= level) Monad[M].pure(println(s"[INFO:$level] $message"))
    else Monad[M].unit

  def warn(message: String): M[Unit] = Monad[M].pure(println(s"[  WARN] $message"))

  def error(message: String): M[Unit] = Monad[M].pure(println(s"[ ERROR] $message"))

}
