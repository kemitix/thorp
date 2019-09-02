package net.kemitix.thorp.lib

import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.throp.uishell.UIEvent
import zio.clock.Clock
import zio.{RIO, UIO}

trait LocalFileSystem {
  def scanCopyUpload(
      uiChannel: UChannel[Any, UIEvent],
      archive: ThorpArchive
  ): RIO[Clock with Hasher with FileSystem with Config with FileScanner,
         Seq[
           StorageQueueEvent
         ]]
}
object LocalFileSystem extends LocalFileSystem {

  override def scanCopyUpload(
      uiChannel: UChannel[Any, UIEvent],
      archive: ThorpArchive
  ): RIO[Clock with Hasher with FileSystem with Config with FileScanner,
         Seq[
           StorageQueueEvent
         ]] =
    for {
      fileSender   <- FileScanner.scanSources
      fileReceiver <- fileReceiver(uiChannel)
      _            <- MessageChannel.pointToPoint(fileSender)(fileReceiver).runDrain
      events       <- UIO(List.empty)
    } yield events

  private def fileReceiver(
      uiChannel: UChannel[Any, UIEvent]
  ): UIO[MessageChannel.UReceiver[Clock, FileScanner.ScannedFile]] =
    UIO { message =>
      val localFile = message.body
      for {
        fileFoundMessage <- Message.create(UIEvent.FileFound(localFile))
        _                <- MessageChannel.send(uiChannel)(fileFoundMessage)

      } yield ()
    }
}
