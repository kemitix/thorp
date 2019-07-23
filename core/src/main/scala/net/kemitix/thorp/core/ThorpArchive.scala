package net.kemitix.thorp.core

import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.{LocalFile, RemoteKey, StorageQueueEvent}
import zio.TaskR

import scala.io.AnsiColor._

trait ThorpArchive {

  def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[MyConsole, StorageQueueEvent]

  def logFileUploaded(
      localFile: LocalFile,
      batchMode: Boolean
  ): TaskR[MyConsole, Unit] =
    for {
      _ <- TaskR.when(batchMode)(
        putStrLn(s"Uploaded: ${localFile.remoteKey.key}"))
      _ <- TaskR.when(!batchMode)(putStrLn(
        s"${GREEN}Uploaded:$RESET ${localFile.remoteKey.key}$eraseToEndOfScreen"))
    } yield ()

  def logFileCopied(
      sourceKey: RemoteKey,
      targetKey: RemoteKey,
      batchMode: Boolean
  ): TaskR[MyConsole, Unit] =
    for {
      _ <- TaskR.when(batchMode)(putStrLn(s"Copied: $sourceKey => $targetKey"))
      _ <- TaskR.when(!batchMode)(
        putStrLn(
          s"${GREEN}Copied:$RESET ${sourceKey.key} => ${targetKey.key}$eraseToEndOfScreen")
      )
    } yield ()

  def logFileDeleted(remoteKey: RemoteKey,
                     batchMode: Boolean): TaskR[MyConsole, Unit] =
    for {
      _ <- TaskR.when(batchMode)(putStrLn(s"Deleted: $remoteKey"))
      _ <- TaskR.when(!batchMode)(
        putStrLn(s"${GREEN}Deleted:$RESET ${remoteKey.key}"))
    } yield ()

}
