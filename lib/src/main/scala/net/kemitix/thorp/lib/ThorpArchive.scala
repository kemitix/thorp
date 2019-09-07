package net.kemitix.thorp.lib

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
import zio.{RIO, ZIO}

trait ThorpArchive {

  def update(
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): ZIO[Storage with Config, Nothing, StorageEvent]

  def logEvent(event: StorageEvent): RIO[Console with Config, StorageEvent] =
    for {
      batchMode <- Config.batchMode
      sqe <- event match {
        case UploadEvent(remoteKey, _) =>
          ZIO(event) <* Console.putMessageLnB(UploadComplete(remoteKey),
                                              batchMode)
        case CopyEvent(sourceKey, targetKey) =>
          ZIO(event) <* Console.putMessageLnB(
            CopyComplete(sourceKey, targetKey),
            batchMode)
        case DeleteEvent(remoteKey) =>
          ZIO(event) <* Console.putMessageLnB(DeleteComplete(remoteKey),
                                              batchMode)
        case ErrorEvent(action, _, e) =>
          ZIO(event) <* Console.putMessageLnB(
            ErrorQueueEventOccurred(action, e),
            batchMode)
        case DoNothingEvent(_) => ZIO(event)
        case ShutdownEvent()   => ZIO(event)
      }
    } yield sqe

}
