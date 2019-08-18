package net.kemitix.thorp.lib

import net.kemitix.thorp.domain.SimpleLens

final case class Counters(
    uploaded: Int,
    deleted: Int,
    copied: Int,
    errors: Int
)

object Counters {
  val empty: Counters = Counters(0, 0, 0, 0)
  val uploaded: SimpleLens[Counters, Int] =
    SimpleLens[Counters, Int](_.uploaded, b => a => b.copy(uploaded = a))
  val deleted: SimpleLens[Counters, Int] =
    SimpleLens[Counters, Int](_.deleted, b => a => b.copy(deleted = a))
  val copied: SimpleLens[Counters, Int] =
    SimpleLens[Counters, Int](_.copied, b => a => b.copy(copied = a))
  val errors: SimpleLens[Counters, Int] =
    SimpleLens[Counters, Int](_.errors, b => a => b.copy(errors = a))
}
