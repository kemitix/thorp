package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.Storage
import zio.{Task, TaskR}

case class UnversionedMirrorArchive(syncTotals: SyncTotals)
    extends ThorpArchive {

  override def update(
      index: Int,
      action: Action,
      totalBytesSoFar: Long
  ): TaskR[Storage with Console with Config, StorageQueueEvent] =
    action match {
      case ToUpload(bucket, localFile, _) =>
        doUpload(index, totalBytesSoFar, bucket, localFile) >>= logEvent
      case ToCopy(bucket, sourceKey, hash, targetKey, _) =>
        Storage.copy(bucket, sourceKey, hash, targetKey) >>= logEvent
      case ToDelete(bucket, remoteKey, _) =>
        Storage.delete(bucket, remoteKey) >>= logEvent
      case DoNothing(_, remoteKey, _) =>
        Task(DoNothingQueueEvent(remoteKey))
    }

  private def doUpload(
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    Storage.upload(
      localFile,
      bucket,
      UploadEventListener(localFile, index, syncTotals, totalBytesSoFar),
      1)
}

object UnversionedMirrorArchive {
  def default(syncTotals: SyncTotals): Task[ThorpArchive] =
    Task {
      new UnversionedMirrorArchive(syncTotals)
    }
}
