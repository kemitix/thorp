package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.StorageEvent.DoNothingEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.{UIEvent, UploadEventListener}
import zio.{UIO, ZIO}

trait UnversionedMirrorArchive extends ThorpArchive {

  override def update(
      uiChannel: UChannel[Any, UIEvent],
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): ZIO[Storage with Config, Nothing, StorageEvent] = {
    val action = sequencedAction.action
    val index  = sequencedAction.index
    val bucket = action.getBucket
    action match {
      case upload: ToUpload =>
        val localFile = upload.getLocalFile
        doUpload(uiChannel, index, totalBytesSoFar, bucket, localFile)
      case toCopy: ToCopy =>
        val sourceKey = toCopy.getSourceKey
        val hash      = toCopy.getHash
        val targetKey = toCopy.getTargetKey
        Storage.copy(bucket, sourceKey, hash, targetKey)
      case toDelete: ToDelete =>
        val remoteKey = toDelete.getRemoteKey
        Storage.delete(bucket, remoteKey)
      case doNothing: Action.DoNothing =>
        val remoteKey = doNothing.getRemoteKey
        UIO(DoNothingEvent(remoteKey))
    }
  }

  private def doUpload(
      uiChannel: UChannel[Any, UIEvent],
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    for {
      settings <- listenerSettings(uiChannel,
                                   index,
                                   totalBytesSoFar,
                                   bucket,
                                   localFile)
      upload <- Storage.upload(localFile, bucket, settings)
    } yield upload

  private def listenerSettings(
      uiChannel: UChannel[Any, UIEvent],
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    for {
      batchMode <- Config.batchMode
    } yield
      UploadEventListener.Settings(uiChannel,
                                   localFile,
                                   index,
                                   totalBytesSoFar,
                                   batchMode)

}

object UnversionedMirrorArchive extends UnversionedMirrorArchive
