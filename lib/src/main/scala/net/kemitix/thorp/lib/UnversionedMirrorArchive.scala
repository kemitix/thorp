package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.domain.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.{UIEvent, UploadEventListener}

trait UnversionedMirrorArchive extends ThorpArchive {

  override def update(configuration: Configuration,
                      uiSink: Channel.Sink[UIEvent],
                      sequencedAction: SequencedAction,
                      totalBytesSoFar: Long): StorageEvent = {
    val action = sequencedAction.action
    val index = sequencedAction.index
    val bucket = action.bucket
    action match {
      case upload: ToUpload =>
        val localFile = upload.localFile
        doUpload(
          configuration,
          uiSink,
          index,
          totalBytesSoFar,
          bucket,
          localFile
        )
      case toCopy: ToCopy =>
        val sourceKey = toCopy.sourceKey
        val hash = toCopy.hash
        val targetKey = toCopy.targetKey
        Storage
          .getInstance()
          .copy(bucket, sourceKey, hash, targetKey)
      case toDelete: ToDelete =>
        val remoteKey = toDelete.remoteKey
        Storage.getInstance().delete(bucket, remoteKey)
      case doNothing: Action.DoNothing =>
        val remoteKey = doNothing.remoteKey
        StorageEvent.doNothingEvent(remoteKey)
    }
  }

  private def doUpload(configuration: Configuration,
                       uiSink: Channel.Sink[UIEvent],
                       index: Int,
                       totalBytesSoFar: Long,
                       bucket: Bucket,
                       localFile: LocalFile) =
    Storage
      .getInstance()
      .upload(
        localFile,
        bucket,
        listenerSettings(
          configuration,
          uiSink,
          index,
          totalBytesSoFar,
          bucket,
          localFile
        )
      )

  private def listenerSettings(configuration: Configuration,
                               uiSink: Channel.Sink[UIEvent],
                               index: Int,
                               totalBytesSoFar: Long,
                               bucket: Bucket,
                               localFile: LocalFile) =
    UploadEventListener.settings(
      uiSink,
      localFile,
      index,
      totalBytesSoFar,
      configuration.batchMode
    )

}

object UnversionedMirrorArchive extends UnversionedMirrorArchive
