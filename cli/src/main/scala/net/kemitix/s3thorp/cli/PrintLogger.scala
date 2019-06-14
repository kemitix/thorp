package net.kemitix.s3thorp.cli

import cats.Monad
import net.kemitix.s3thorp.domain.Logger

class PrintLogger[M[_]: Monad](verbosity: Int) extends Logger[M] {

  override def debug(message: => String): M[Unit] = Monad[M].pure(println(s"[ DEBUG] $message"))

  override def info(message: => String): M[Unit] = Monad[M].pure(println(s"[  INFO] $message"))

  override def warn(message: String): M[Unit] = Monad[M].pure(println(s"[  WARN] $message"))

  override def error(message: String): M[Unit] = Monad[M].pure(println(s"[ ERROR] $message"))

}
