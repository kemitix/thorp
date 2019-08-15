package net.kemitix.thorp.core

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
  ): RIO[Storage with Console with Config, StorageQueueEvent]

  def logEvent(
      event: StorageQueueEvent): RIO[Console with Config, StorageQueueEvent] =
    event match {
      case UploadQueueEvent(remoteKey, _) =>
        ZIO(event) <* Console.putMessageLnB(UploadComplete(remoteKey))
      case CopyQueueEvent(sourceKey, targetKey) =>
        ZIO(event) <* Console.putMessageLnB(CopyComplete(sourceKey, targetKey))
      case DeleteQueueEvent(remoteKey) =>
        ZIO(event) <* Console.putMessageLnB(DeleteComplete(remoteKey))
      case ErrorQueueEvent(action, _, e) =>
        ZIO(event) <* Console.putMessageLnB(ErrorQueueEventOccurred(action, e))
      case DoNothingQueueEvent(_) => ZIO(event)
      case ShutdownQueueEvent()   => ZIO(event)
    }

}
