package net.kemitix.thorp.core

import net.kemitix.thorp.domain.SyncTotals

final case class SyncPlan private (
    actions: LazyList[Action],
    syncTotals: SyncTotals
)

object SyncPlan {
  val empty: SyncPlan = SyncPlan(LazyList.empty, SyncTotals.empty)
  def create(actions: LazyList[Action], syncTotals: SyncTotals): SyncPlan =
    SyncPlan(actions, syncTotals)
}
