package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.domain.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.{UIEvent, UploadEventListener}
import zio.{UIO, ZIO}

trait UnversionedMirrorArchive extends ThorpArchive {

  override def update(
      configuration: Configuration,
      uiChannel: UChannel[Any, UIEvent],
      sequencedAction: SequencedAction,
      totalBytesSoFar: Long
  ): ZIO[Storage, Nothing, StorageEvent] = {
    val action = sequencedAction.action
    val index  = sequencedAction.index
    val bucket = action.bucket
    action match {
      case upload: ToUpload =>
        val localFile = upload.localFile
        doUpload(configuration,
                 uiChannel,
                 index,
                 totalBytesSoFar,
                 bucket,
                 localFile)
      case toCopy: ToCopy =>
        val sourceKey = toCopy.sourceKey
        val hash      = toCopy.hash
        val targetKey = toCopy.targetKey
        Storage.copy(bucket, sourceKey, hash, targetKey)
      case toDelete: ToDelete =>
        val remoteKey = toDelete.remoteKey
        Storage.delete(bucket, remoteKey)
      case doNothing: Action.DoNothing =>
        val remoteKey = doNothing.remoteKey
        UIO(StorageEvent.doNothingEvent(remoteKey))
    }
  }

  private def doUpload(
      configuration: Configuration,
      uiChannel: UChannel[Any, UIEvent],
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    Storage.upload(localFile,
                   bucket,
                   listenerSettings(configuration,
                                    uiChannel,
                                    index,
                                    totalBytesSoFar,
                                    bucket,
                                    localFile))

  private def listenerSettings(
      configuration: Configuration,
      uiChannel: UChannel[Any, UIEvent],
      index: Int,
      totalBytesSoFar: Long,
      bucket: Bucket,
      localFile: LocalFile
  ) =
    UploadEventListener.Settings(uiChannel,
                                 localFile,
                                 index,
                                 totalBytesSoFar,
                                 configuration.batchMode)

}

object UnversionedMirrorArchive extends UnversionedMirrorArchive
