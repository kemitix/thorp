package net.kemitix.thorp.core

import monocle.macros.Lenses
import net.kemitix.thorp.domain.SyncTotals

@Lenses
case class SyncPlan(
    actions: Stream[Action] = Stream(),
    syncTotals: SyncTotals = SyncTotals()
)
