package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.domain.StorageQueueEvent

trait ThorpArchive {

  def update(action: Action): Stream[IO[StorageQueueEvent]]

}
