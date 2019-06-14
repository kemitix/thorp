package net.kemitix.s3thorp.core

import cats.Monad
import net.kemitix.s3thorp.domain.Logger

class DummyLogger[M[_]: Monad] extends Logger[M] {

  override def debug(message: => String): M[Unit] = Monad[M].unit

  override def info(message: =>String): M[Unit] = Monad[M].unit

  override def warn(message: String): M[Unit] = Monad[M].unit

  override def error(message: String): M[Unit] = Monad[M].unit

}
