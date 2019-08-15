package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import zio.{RIO, Task}

final case class UnversionedMirrorArchive(syncTotals: SyncTotals)
    extends ThorpArchive {

  override def update(
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): RIO[Storage with Console with Config, StorageQueueEvent] =
    sequencedAction match {
      case SequencedAction(ToUpload(bucket, localFile, _), index) =>
        doUpload(index, totalBytesSoFar, bucket, localFile) >>= logEvent
      case SequencedAction(ToCopy(bucket, sourceKey, hash, targetKey, _), _) =>
        Storage.copy(bucket, sourceKey, hash, targetKey) >>= logEvent
      case SequencedAction(ToDelete(bucket, remoteKey, _), _) =>
        Storage.delete(bucket, remoteKey) >>= logEvent
      case SequencedAction(DoNothing(_, remoteKey, _), _) =>
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
      UploadEventListener.Settings(
        localFile,
        index,
        syncTotals,
        totalBytesSoFar
      )
    )
}

object UnversionedMirrorArchive {
  def default(syncTotals: SyncTotals): Task[ThorpArchive] =
    Task {
      new UnversionedMirrorArchive(syncTotals)
    }
}
