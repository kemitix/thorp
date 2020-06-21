package net.kemitix.thorp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.filesystem.FileSystem
import zio.clock.Clock

sealed trait ProgressEvent

object ProgressEvent {
  type Env = Console
  type ProgressSender =
    MessageChannel.ESender[Clock with FileSystem, Throwable, ProgressEvent]
  type ProgressReceiver =
    MessageChannel.Receiver[ProgressEvent.Env, ProgressEvent]
  type ProgressChannel = MessageChannel.Channel[Console, ProgressEvent]

  final case class PingEvent() extends ProgressEvent
}
