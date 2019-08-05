package net.kemitix.thorp.core

final case class SequencedAction(
    action: Action,
    index: Int
)
