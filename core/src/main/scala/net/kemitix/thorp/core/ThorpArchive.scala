package net.kemitix.thorp.core

import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.domain.StorageQueueEvent.{
  CopyQueueEvent,
  DeleteQueueEvent,
  DoNothingQueueEvent,
  ErrorQueueEvent,
  ShutdownQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain.Terminal._
import zio.TaskR

import scala.io.AnsiColor._

trait ThorpArchive {

  def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[Console, StorageQueueEvent]

  def logEvent(
      event: StorageQueueEvent,
      batchMode: Boolean
  ): TaskR[Console, Unit] =
    event match {
      case UploadQueueEvent(remoteKey, _) =>
        for {
          _ <- TaskR.when(batchMode)(putStrLn(s"Uploaded: ${remoteKey.key}"))
          _ <- TaskR.when(!batchMode)(
            putStrLn(
              s"${GREEN}Uploaded:$RESET ${remoteKey.key}$eraseToEndOfScreen"))
        } yield ()
      case CopyQueueEvent(sourceKey, targetKey) =>
        for {
          _ <- TaskR.when(batchMode)(
            putStrLn(s"Copied: ${sourceKey.key} => ${targetKey.key}"))
          _ <- TaskR.when(!batchMode)(
            putStrLn(
              s"${GREEN}Copied:$RESET ${sourceKey.key} => ${targetKey.key}$eraseToEndOfScreen")
          )
        } yield ()
      case DeleteQueueEvent(remoteKey) =>
        for {
          _ <- TaskR.when(batchMode)(putStrLn(s"Deleted: $remoteKey"))
          _ <- TaskR.when(!batchMode)(
            putStrLn(
              s"${GREEN}Deleted:$RESET ${remoteKey.key}$eraseToEndOfScreen"))
        } yield ()
      case ErrorQueueEvent(action, _, e) =>
        for {
          _ <- TaskR.when(batchMode)(
            putStrLn(s"${action.name} failed: ${action.keys}: ${e.getMessage}"))
          _ <- TaskR.when(!batchMode)(putStrLn(
            s"${RED}ERROR:$RESET ${action.name} ${action.keys}: ${e.getMessage}$eraseToEndOfScreen"))
        } yield ()
      case DoNothingQueueEvent(_) => TaskR(())
      case ShutdownQueueEvent()   => TaskR(())
    }

}
