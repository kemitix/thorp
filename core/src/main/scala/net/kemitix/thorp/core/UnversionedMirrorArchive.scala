package net.kemitix.thorp.core

import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage._
import net.kemitix.thorp.storage.api.Storage
import zio.{Task, TaskR}

case class UnversionedMirrorArchive(
    batchMode: Boolean,
    syncTotals: SyncTotals
) extends ThorpArchive {

  override def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[Storage with Console, StorageQueueEvent] =
    action match {
      case ToUpload(bucket, localFile, _) =>
        for {
          event <- doUpload(index, totalBytesSoFar, bucket, localFile)
          _     <- logEvent(event, batchMode)
        } yield event
      case ToCopy(bucket, sourceKey, hash, targetKey, _) =>
        for {
          event <- copyObject(bucket, sourceKey, hash, targetKey)
          _     <- logEvent(event, batchMode)
        } yield event
      case ToDelete(bucket, remoteKey, _) =>
        for {
          event <- deleteObject(bucket, remoteKey)
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
    upload(localFile,
           bucket,
           batchMode,
           UploadEventListener(localFile, index, syncTotals, totalBytesSoFar),
           1)
}

object UnversionedMirrorArchive {
  def default(
      batchMode: Boolean,
      syncTotals: SyncTotals
  ): ThorpArchive =
    new UnversionedMirrorArchive(batchMode, syncTotals)
}
