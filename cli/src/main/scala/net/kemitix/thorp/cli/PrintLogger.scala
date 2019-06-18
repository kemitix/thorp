package net.kemitix.thorp.cli

import cats.Monad
import net.kemitix.thorp.domain.Logger

class PrintLogger[M[_]: Monad](isDebug: Boolean = false) extends Logger[M] {

  override def debug(message: => String): M[Unit] =
    if (isDebug) Monad[M].pure(println(s"[ DEBUG] $message"))
    else Monad[M].unit

  override def info(message: => String): M[Unit] = Monad[M].pure(println(s"[  INFO] $message"))

  override def warn(message: String): M[Unit] = Monad[M].pure(println(s"[  WARN] $message"))

  override def error(message: String): M[Unit] = Monad[M].pure(println(s"[ ERROR] $message"))

  override def withDebug(debug: Boolean): Logger[M] = if (isDebug == debug) this else new PrintLogger[M](debug)

}
