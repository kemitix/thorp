package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain.{
  Bucket,
  LocalFile,
  Logger,
  StorageQueueEvent,
  SyncTotals,
  UploadEventListener
}
import net.kemitix.thorp.storage.api.StorageService

case class UnversionedMirrorArchive(
    storageService: StorageService,
    batchMode: Boolean,
    syncTotals: SyncTotals
) extends ThorpArchive {

  override def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  )(implicit l: Logger): Stream[IO[StorageQueueEvent]] =
    Stream(action match {
      case ToUpload(bucket, localFile, _) =>
        for {
          event <- doUpload(index, totalBytesSoFar, bucket, localFile)
          _     <- logFileUploaded(localFile, batchMode)
        } yield event
      case ToCopy(bucket, sourceKey, hash, targetKey, _) =>
        for {
          event <- storageService.copy(bucket, sourceKey, hash, targetKey)
        } yield event
      case ToDelete(bucket, remoteKey, _) =>
        for {
          event <- storageService.delete(bucket, remoteKey)
        } yield event
      case DoNothing(_, remoteKey, _) =>
        IO.pure(DoNothingQueueEvent(remoteKey))
    })

  private def doUpload(
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    storageService.upload(
      localFile,
      bucket,
      batchMode,
      UploadEventListener(localFile, index, syncTotals, totalBytesSoFar),
      1)
}

object UnversionedMirrorArchive {
  def default(
      storageService: StorageService,
      batchMode: Boolean,
      syncTotals: SyncTotals
  ): ThorpArchive =
    new UnversionedMirrorArchive(storageService, batchMode, syncTotals)
}
