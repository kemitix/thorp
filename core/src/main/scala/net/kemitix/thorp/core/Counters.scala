package net.kemitix.thorp.core

import monocle.Lens
import monocle.macros.GenLens

final case class Counters(
    uploaded: Int = 0,
    deleted: Int = 0,
    copied: Int = 0,
    errors: Int = 0
)

object Counters {
  val uploaded: Lens[Counters, Int] = GenLens[Counters](_.uploaded)
  val deleted: Lens[Counters, Int]  = GenLens[Counters](_.deleted)
  val copied: Lens[Counters, Int]   = GenLens[Counters](_.copied)
  val errors: Lens[Counters, Int]   = GenLens[Counters](_.errors)
}
