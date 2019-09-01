package net.kemitix.throp.uishell

sealed trait UIEvent
object UIEvent {
  final case class Ping() extends UIEvent
}
