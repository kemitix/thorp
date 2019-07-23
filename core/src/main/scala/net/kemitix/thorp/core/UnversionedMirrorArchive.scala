package net.kemitix.thorp.core

import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.StorageService
import zio.{Task, TaskR}

case class UnversionedMirrorArchive(
    storageService: StorageService,
    batchMode: Boolean,
    syncTotals: SyncTotals
) extends ThorpArchive {

  override def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[MyConsole, StorageQueueEvent] =
    action match {
      case ToUpload(bucket, localFile, _) =>
        for {
          event <- doUpload(index, totalBytesSoFar, bucket, localFile)
          _     <- logEvent(event, batchMode)
        } yield event
      case ToCopy(bucket, sourceKey, hash, targetKey, _) =>
        for {
          event <- storageService.copy(bucket, sourceKey, hash, targetKey)
          _     <- logEvent(event, batchMode)
        } yield event
      case ToDelete(bucket, remoteKey, _) =>
        for {
          event <- storageService.delete(bucket, remoteKey)
          _     <- logEvent(event, batchMode)
        } yield event
      case DoNothing(_, remoteKey, _) =>
        Task(DoNothingQueueEvent(remoteKey))
    }

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
