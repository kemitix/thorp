package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.ConsoleOut.{
  CopyComplete,
  DeleteComplete,
  ErrorQueueEventOccurred,
  UploadComplete
}
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.domain.StorageQueueEvent._
import net.kemitix.thorp.storage.Storage
import zio.{RIO, ZIO}

trait ThorpArchive {

  def update(
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): ZIO[Storage with Config, Nothing, StorageQueueEvent]

  def logEvent(
      event: StorageQueueEvent): RIO[Console with Config, StorageQueueEvent] =
    for {
      batchMode <- Config.batchMode
      sqe <- event match {
        case UploadQueueEvent(remoteKey, _) =>
          ZIO(event) <* Console.putMessageLnB(UploadComplete(remoteKey),
                                              batchMode)
        case CopyQueueEvent(sourceKey, targetKey) =>
          ZIO(event) <* Console.putMessageLnB(
            CopyComplete(sourceKey, targetKey),
            batchMode)
        case DeleteQueueEvent(remoteKey) =>
          ZIO(event) <* Console.putMessageLnB(DeleteComplete(remoteKey),
                                              batchMode)
        case ErrorQueueEvent(action, _, e) =>
          ZIO(event) <* Console.putMessageLnB(
            ErrorQueueEventOccurred(action, e),
            batchMode)
        case DoNothingQueueEvent(_) => ZIO(event)
        case ShutdownQueueEvent()   => ZIO(event)
      }
    } yield sqe

}
