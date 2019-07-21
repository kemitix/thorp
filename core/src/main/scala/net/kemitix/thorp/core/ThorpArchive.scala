package net.kemitix.thorp.core

import net.kemitix.thorp.domain.{LocalFile, StorageQueueEvent}
import zio.TaskR
import zio.console._

trait ThorpArchive {

  def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[Console, StorageQueueEvent]

  def logFileUploaded(
      localFile: LocalFile,
      batchMode: Boolean
  ): TaskR[Console, Unit] =
    for {
      _ <- TaskR.when(batchMode)(
        putStrLn(s"Uploaded: ${localFile.remoteKey.key}"))
    } yield ()

}
