package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageEvent.DoNothingEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import zio.{UIO, ZIO}

trait UnversionedMirrorArchive extends ThorpArchive {

  override def update(
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): ZIO[Storage with Config, Nothing, StorageEvent] =
    sequencedAction match {
      case SequencedAction(ToUpload(bucket, localFile, _), index) =>
        doUpload(index, totalBytesSoFar, bucket, localFile)
      case SequencedAction(ToCopy(bucket, sourceKey, hash, targetKey, _), _) =>
        Storage.copy(bucket, sourceKey, hash, targetKey)
      case SequencedAction(ToDelete(bucket, remoteKey, _), _) =>
        Storage.delete(bucket, remoteKey)
      case SequencedAction(DoNothing(_, remoteKey, _), _) =>
        UIO(DoNothingEvent(remoteKey))
    }

  private def doUpload(
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    for {
      batchMode <- Config.batchMode
      upload <- Storage.upload(
        localFile,
        bucket,
        UploadEventListener.Settings(
          localFile,
          index,
          totalBytesSoFar,
          batchMode
        )
      )
    } yield upload
}

object UnversionedMirrorArchive extends UnversionedMirrorArchive
