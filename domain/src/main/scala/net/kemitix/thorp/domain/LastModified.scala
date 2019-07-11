package net.kemitix.thorp.domain

import java.time.Instant

final case class LastModified(when: Instant = Instant.now)
