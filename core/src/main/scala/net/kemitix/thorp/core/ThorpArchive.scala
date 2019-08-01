package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.domain.StorageQueueEvent._
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.storage.api.Storage
import zio.{TaskR, ZIO}

import scala.io.AnsiColor._

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
        ZIO(event) <* logMessage(
          s"Uploaded: ${remoteKey.key}",
          s"${GREEN}Uploaded:$RESET ${remoteKey.key}$eraseToEndOfScreen")
      case CopyQueueEvent(sourceKey, targetKey) =>
        ZIO(event) <* logMessage(
          s"Copied: ${sourceKey.key} => ${targetKey.key}",
          s"${GREEN}Copied:$RESET ${sourceKey.key} => ${targetKey.key}$eraseToEndOfScreen")
      case DeleteQueueEvent(remoteKey) =>
        ZIO(event) <* logMessage(
          s"Deleted: $remoteKey",
          s"${GREEN}Deleted:$RESET ${remoteKey.key}$eraseToEndOfScreen")
      case ErrorQueueEvent(action, _, e) =>
        ZIO(event) <* logMessage(
          s"${action.name} failed: ${action.keys}: ${e.getMessage}",
          s"${RED}ERROR:$RESET ${action.name} ${action.keys}: ${e.getMessage}$eraseToEndOfScreen")
      case DoNothingQueueEvent(_) => TaskR(event)
      case ShutdownQueueEvent()   => TaskR(event)
    }

  private def logMessage(
      batchModeMessage: String,
      normalMessage: String
  ): ZIO[Console with Config, Nothing, Unit] = {
    for {
      batchMode <- Config.batchMode
      _ <- alternative(batchMode)(
        Console.putStrLn(batchModeMessage),
        Console.putStrLn(normalMessage)
      )
    } yield ()
  }

  private def alternative[R, E, A](b: Boolean)(
      whenTrue: ZIO[R, E, A],
      whenFalse: ZIO[R, E, A]
  ) =
    if (b) whenTrue.const(())
    else whenFalse.const(())

}
