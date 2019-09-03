package net.kemitix.thorp.lib

import net.kemitix.thorp.domain.Action

final case class SequencedAction(
    action: Action,
    index: Int
)
