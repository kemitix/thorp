package net.kemitix.thorp.core

case class SyncPlan(actions: Stream[Action] = Stream(),
                    count: Long = 0L,
                    totalSizeBytes: Long = 0L)
