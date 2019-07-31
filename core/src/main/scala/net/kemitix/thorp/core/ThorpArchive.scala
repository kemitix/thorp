package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
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
import net.kemitix.thorp.storage.api.Storage
import zio.TaskR

import scala.io.AnsiColor._

trait ThorpArchive {

  def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[Storage with Console with Config, StorageQueueEvent]

  def logEvent(
      event: StorageQueueEvent
  ): TaskR[Console with Config, StorageQueueEvent] =
    event match {
      case UploadQueueEvent(remoteKey, _) =>
        for {
          batchMode <- Config.batchMode
          _ <- TaskR.when(batchMode)(
            Console.putStrLn(s"Uploaded: ${remoteKey.key}"))
          _ <- TaskR.when(!batchMode)(
            Console.putStrLn(
              s"${GREEN}Uploaded:$RESET ${remoteKey.key}$eraseToEndOfScreen"))
        } yield event
      case CopyQueueEvent(sourceKey, targetKey) =>
        for {
          batchMode <- Config.batchMode
          _ <- TaskR.when(batchMode)(
            Console.putStrLn(s"Copied: ${sourceKey.key} => ${targetKey.key}"))
          _ <- TaskR.when(!batchMode)(
            Console.putStrLn(
              s"${GREEN}Copied:$RESET ${sourceKey.key} => ${targetKey.key}$eraseToEndOfScreen")
          )
        } yield event
      case DeleteQueueEvent(remoteKey) =>
        for {
          batchMode <- Config.batchMode
          _         <- TaskR.when(batchMode)(Console.putStrLn(s"Deleted: $remoteKey"))
          _ <- TaskR.when(!batchMode)(
            Console.putStrLn(
              s"${GREEN}Deleted:$RESET ${remoteKey.key}$eraseToEndOfScreen"))
        } yield event
      case ErrorQueueEvent(action, _, e) =>
        for {
          batchMode <- Config.batchMode
          _ <- TaskR.when(batchMode)(
            Console.putStrLn(
              s"${action.name} failed: ${action.keys}: ${e.getMessage}"))
          _ <- TaskR.when(!batchMode)(Console.putStrLn(
            s"${RED}ERROR:$RESET ${action.name} ${action.keys}: ${e.getMessage}$eraseToEndOfScreen"))
        } yield event
      case DoNothingQueueEvent(_) => TaskR(event)
      case ShutdownQueueEvent()   => TaskR(event)
    }

}
