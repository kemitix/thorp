package net.kemitix.throp.uishell

sealed trait UIEvent
object UIEvent {
  case object ShowValidConfig extends UIEvent

  case class RemoteDataFetched(size: Int) extends UIEvent

}
