package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.ConsoleOut.{
  CopyComplete,
  DeleteComplete,
  ErrorQueueEventOccurred,
  UploadComplete
}
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageEvent
import net.kemitix.thorp.domain.StorageEvent._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.UIEvent
import zio.{RIO, ZIO}

trait ThorpArchive {

  def update(
      uiChannel: UChannel[Any, UIEvent],
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): ZIO[Storage with Config, Nothing, StorageEvent]

  def logEvent(event: StorageEvent): RIO[Console with Config, StorageEvent] =
    for {
      batchMode <- Config.batchMode
      sqe <- event match {
        case uploadEvent: UploadEvent => {
          val remoteKey = uploadEvent.remoteKey
          ZIO(event) <* Console.putMessageLnB(UploadComplete(remoteKey),
                                              batchMode)
        }
        case copyEvent: CopyEvent => {
          val sourceKey = copyEvent.sourceKey
          val targetKey = copyEvent.targetKey
          ZIO(event) <* Console.putMessageLnB(
            CopyComplete(sourceKey, targetKey),
            batchMode)
        }
        case deleteEvent: DeleteEvent => {
          val remoteKey = deleteEvent.remoteKey
          ZIO(event) <* Console.putMessageLnB(DeleteComplete(remoteKey),
                                              batchMode)
        }
        case errorEvent: ErrorEvent => {
          val action = errorEvent.action
          val e      = errorEvent.e
          ZIO(event) <* Console.putMessageLnB(
            ErrorQueueEventOccurred(action, e),
            batchMode)
        }
        case _ => ZIO(event)
      }
    } yield sqe

}
