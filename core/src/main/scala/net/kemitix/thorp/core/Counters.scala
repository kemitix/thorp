package net.kemitix.thorp.core

final case class Counters(
    uploaded: Int = 0,
    deleted: Int = 0,
    copied: Int = 0,
    errors: Int = 0
)
