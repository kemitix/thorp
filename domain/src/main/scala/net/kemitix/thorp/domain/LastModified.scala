package net.kemitix.thorp.domain

import java.time.Instant

final case class LastModified(when: Instant)

object LastModified {
  def now: LastModified = LastModified(Instant.now)
}
