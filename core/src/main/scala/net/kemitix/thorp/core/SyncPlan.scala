package net.kemitix.thorp.core

import net.kemitix.thorp.domain.SyncTotals

case class SyncPlan(
    actions: Stream[Action] = Stream(),
    syncTotals: SyncTotals = SyncTotals()
)
