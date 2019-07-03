package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain.{Logger, StorageQueueEvent, SyncTotals, UploadEventListener}
import net.kemitix.thorp.storage.api.StorageService

case class UnversionedMirrorArchive(storageService: StorageService,
                                    batchMode: Boolean,
                                    syncTotals: SyncTotals) extends ThorpArchive {
  override def update(index: Int,
                      action: Action,
                      totalBytesSoFar: Long)
                     (implicit l: Logger): Stream[IO[StorageQueueEvent]] =
    Stream(
      action match {
        case ToUpload(bucket, localFile, size) =>
          for {
            event <- storageService.upload(localFile, bucket, batchMode,
              new UploadEventListener(localFile, index, syncTotals, totalBytesSoFar), 1)
            _ <- fileUploaded(localFile, batchMode)
          } yield event
        case ToCopy(bucket, sourceKey, hash, targetKey, size) =>
          for {
            event <- storageService.copy(bucket, sourceKey, hash, targetKey)
          } yield event
        case ToDelete(bucket, remoteKey, size) =>
          for {
            event <- storageService.delete(bucket, remoteKey)
          } yield event
        case DoNothing(_, remoteKey, size) =>
          IO.pure(DoNothingQueueEvent(remoteKey))
      })

}

object UnversionedMirrorArchive {
  def default(storageService: StorageService,
              batchMode: Boolean,
              syncTotals: SyncTotals): ThorpArchive =
    new UnversionedMirrorArchive(storageService, batchMode, syncTotals)
}
