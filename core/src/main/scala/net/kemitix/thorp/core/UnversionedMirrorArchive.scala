package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain.{StorageQueueEvent, UploadEventListener}
import net.kemitix.thorp.storage.api.StorageService

case class UnversionedMirrorArchive(storageService: StorageService) extends ThorpArchive {
  override def update(action: Action): Stream[IO[StorageQueueEvent]] =
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          for {
            event <- storageService.upload(localFile, bucket, new UploadEventListener(localFile), 1)
          } yield event
        case ToCopy(bucket, sourceKey, hash, targetKey) =>
          for {
            event <- storageService.copy(bucket, sourceKey, hash, targetKey)
          } yield event
        case ToDelete(bucket, remoteKey) =>
          for {
            event <- storageService.delete(bucket, remoteKey)
          } yield event
        case DoNothing(_, remoteKey) =>
          IO.pure(DoNothingQueueEvent(remoteKey))
      })

}

object UnversionedMirrorArchive {
  def default(storageService: StorageService): ThorpArchive =
    new UnversionedMirrorArchive(storageService)
}
