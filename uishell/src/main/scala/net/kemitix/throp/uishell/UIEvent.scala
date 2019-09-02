package net.kemitix.throp.uishell

import net.kemitix.thorp.domain.Counters

sealed trait UIEvent
object UIEvent {
  case object ShowValidConfig extends UIEvent

  case class RemoteDataFetched(size: Int) extends UIEvent

  case class ShowSummary(counters: Counters) extends UIEvent

}
