package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.throp.uishell.UIEvent
import zio.{RIO, UIO}
import zio.clock.Clock

object PushLocalChanges {

  type UIChannel = UChannel[Any, UIEvent]

  def apply(uiChannel: UIChannel,
            archive: ThorpArchive): RIO[Clock, Seq[StorageQueueEvent]] =
    for {
      events <- UIO(List.empty)
    } yield events

}
