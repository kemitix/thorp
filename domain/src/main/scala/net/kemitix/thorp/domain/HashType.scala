package net.kemitix.thorp.domain

import zio.IO

trait HashType

object HashType {
  def from(str: String): IO[Nothing, HashType] = ???

  case object MD5 extends HashType
}
