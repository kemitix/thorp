package net.kemitix.thorp.core

import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.{LocalFile, StorageQueueEvent}
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

}
