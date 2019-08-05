package net.kemitix.thorp.core

import net.kemitix.thorp.domain.SyncTotals

final case class SyncPlan private (
    actions: Stream[Action],
    syncTotals: SyncTotals
)

object SyncPlan {
  val empty: SyncPlan = SyncPlan(Stream.empty, SyncTotals.empty)
  def create(actions: Stream[Action], syncTotals: SyncTotals): SyncPlan =
    SyncPlan(actions, syncTotals)
}
