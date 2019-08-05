package net.kemitix.thorp.core

import net.kemitix.thorp.domain.StorageQueueEvent

final case class EventQueue(
    events: Stream[StorageQueueEvent],
    bytesInQueue: Long
)
