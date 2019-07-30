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
        for {
          event <- doUpload(index, totalBytesSoFar, bucket, localFile)
          _     <- logEvent(event)
        } yield event
      case ToCopy(bucket, sourceKey, hash, targetKey, _) =>
        for {
          event <- Storage.copy(bucket, sourceKey, hash, targetKey)
          _     <- logEvent(event)
        } yield event
      case ToDelete(bucket, remoteKey, _) =>
        for {
          event <- Storage.delete(bucket, remoteKey)
          _     <- logEvent(event)
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
