package net.kemitix.thorp.core

import net.kemitix.thorp.domain.SyncTotals

case class SyncPlan private (
    actions: Stream[Action],
    syncTotals: SyncTotals
)

object SyncPlan {
  def empty: SyncPlan = SyncPlan(Stream.empty, SyncTotals.empty)
  def create(actions: Stream[Action], syncTotals: SyncTotals): SyncPlan =
    SyncPlan(actions, syncTotals)
}
