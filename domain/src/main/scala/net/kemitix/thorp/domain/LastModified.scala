package net.kemitix.thorp.domain

import java.time.Instant

import monocle.macros.Lenses

@Lenses
final case class LastModified(
    when: Instant = Instant.now
)
