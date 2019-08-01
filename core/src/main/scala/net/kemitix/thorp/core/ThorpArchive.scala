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
import net.kemitix.thorp.storage.api.Storage
import zio.{TaskR, ZIO}

trait ThorpArchive {

  def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[Storage with Console with Config, StorageQueueEvent]

  def logEvent(
      event: StorageQueueEvent): TaskR[Console with Config, StorageQueueEvent] =
    event match {
      case UploadQueueEvent(remoteKey, _) =>
        ZIO(event) <* Console.putMessageLn(UploadComplete(remoteKey))
      case CopyQueueEvent(sourceKey, targetKey) =>
        ZIO(event) <* Console.putMessageLn(CopyComplete(sourceKey, targetKey))
      case DeleteQueueEvent(remoteKey) =>
        ZIO(event) <* Console.putMessageLn(DeleteComplete(remoteKey))
      case ErrorQueueEvent(action, _, e) =>
        ZIO(event) <* Console.putMessageLn(ErrorQueueEventOccurred(action, e))
      case DoNothingQueueEvent(_) => ZIO(event)
      case ShutdownQueueEvent()   => ZIO(event)
    }

}
