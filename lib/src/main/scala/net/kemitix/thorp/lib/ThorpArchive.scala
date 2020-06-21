package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageEvent
import net.kemitix.thorp.domain.StorageEvent._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.UIEvent
import zio.{UIO, ZIO}

trait ThorpArchive {

  def update(configuration: Configuration,
             uiChannel: UChannel[Any, UIEvent],
             sequencedAction: SequencedAction,
             totalBytesSoFar: Long): ZIO[Storage, Nothing, StorageEvent]

  def logEvent(configuration: Configuration,
               event: StorageEvent): UIO[StorageEvent] = {
    val batchMode = configuration.batchMode
    for {
      sqe <- event match {
        case uploadEvent: UploadEvent =>
          val remoteKey = uploadEvent.remoteKey
          UIO(event) <* {
            Console.putMessageLnB(
              ConsoleOut.uploadComplete(remoteKey),
              batchMode
            )
            UIO.unit
          }
        case copyEvent: CopyEvent =>
          val sourceKey = copyEvent.sourceKey
          val targetKey = copyEvent.targetKey
          UIO(event) <* {
            Console.putMessageLnB(
              ConsoleOut.copyComplete(sourceKey, targetKey),
              batchMode
            )
            UIO.unit
          }
        case deleteEvent: DeleteEvent =>
          val remoteKey = deleteEvent.remoteKey
          UIO(event) <* {
            Console.putMessageLnB(
              ConsoleOut.deleteComplete(remoteKey),
              batchMode
            )
            UIO.unit
          }
        case errorEvent: ErrorEvent =>
          val action = errorEvent.action
          val e = errorEvent.e
          UIO(event) <* {
            Console.putMessageLnB(
              ConsoleOut.errorQueueEventOccurred(action, e),
              batchMode
            )
            UIO.unit
          }
        case _ => UIO(event)
      }
    } yield sqe
  }

}
