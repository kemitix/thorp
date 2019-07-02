package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.domain.{LocalFile, Logger, StorageQueueEvent}

trait ThorpArchive {

  def update(indexedAction: (Action, Int))(implicit l: Logger): Stream[IO[StorageQueueEvent]]

  def fileUploaded(localFile: LocalFile,
                   batchMode: Boolean)
                  (implicit l: Logger): IO[Unit] =
    if (batchMode) l.info(s"Uploaded: ${localFile.remoteKey.key}") else IO.unit

}
