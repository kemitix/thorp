package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain.{Logger, StorageQueueEvent, UploadEventListener}
import net.kemitix.thorp.storage.api.StorageService

case class UnversionedMirrorArchive(storageService: StorageService,
                                    batchMode: Boolean,
                                    syncPlan: SyncPlan) extends ThorpArchive {
  override def update(indexedAction: (Action, Int))
                     (implicit l: Logger): Stream[IO[StorageQueueEvent]] =
    Stream(
      indexedAction match {
        case (ToUpload(bucket, localFile), index) =>
          for {
            event <- storageService.upload(localFile, bucket, batchMode, new UploadEventListener(localFile), 1)
            _ <- fileUploaded(localFile, batchMode)
          } yield event
        case (ToCopy(bucket, sourceKey, hash, targetKey), index) =>
          for {
            event <- storageService.copy(bucket, sourceKey, hash, targetKey)
          } yield event
        case (ToDelete(bucket, remoteKey), index) =>
          for {
            event <- storageService.delete(bucket, remoteKey)
          } yield event
        case (DoNothing(_, remoteKey), index) =>
          IO.pure(DoNothingQueueEvent(remoteKey))
      })

}

object UnversionedMirrorArchive {
  def default(storageService: StorageService,
              batchMode: Boolean,
              syncPlan: SyncPlan): ThorpArchive =
    new UnversionedMirrorArchive(storageService, batchMode, syncPlan)
}
