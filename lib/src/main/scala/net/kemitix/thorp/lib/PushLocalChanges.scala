package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.throp.uishell.UIEvent
import zio.clock.Clock
import zio.{RIO, UIO}

object PushLocalChanges {

  type UIChannel = UChannel[Any, UIEvent]

  def apply(uiChannel: UIChannel, archive: ThorpArchive)
    : RIO[Clock with Hasher with FileSystem with Config with FileScanner,
          Seq[StorageQueueEvent]] =
    for {
      fileSender   <- FileScanner.scanSources
      fileReceiver <- fileReceiver(uiChannel)
      _            <- MessageChannel.pointToPoint(fileSender)(fileReceiver).runDrain
      events       <- UIO(List.empty)
    } yield events

  private def fileReceiver(uiChannel: UIChannel)
    : UIO[MessageChannel.UReceiver[Any, FileScanner.ScannedFile]] =
    UIO { message =>
      UIO.succeed(())
    }
}
