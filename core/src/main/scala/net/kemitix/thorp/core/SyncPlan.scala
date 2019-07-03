package net.kemitix.thorp.core

case class SyncPlan(actions: Stream[Action] = Stream(),
                    syncTotals: SyncTotals = SyncTotals())
