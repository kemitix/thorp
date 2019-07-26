package net.kemitix.thorp.storage.aws

import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import net.kemitix.thorp.domain.StorageQueueEvent.{
  Action,
  ErrorQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain.UploadEvent.{
  ByteTransferEvent,
  RequestEvent,
  TransferEvent
}
import net.kemitix.thorp.domain.{StorageQueueEvent, _}
import zio.{Task, UIO}

trait Uploader {

  def upload(transferManager: => AmazonTransferManager)(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): UIO[StorageQueueEvent] =
    transfer(transferManager)(localFile, bucket, batchMode, uploadEventListener)
      .catchAll(
        e =>
          UIO.succeed(
            ErrorQueueEvent(Action.Upload(localFile.remoteKey.key),
                            localFile.remoteKey,
                            e)))

  private def transfer(transferManager: => AmazonTransferManager)(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener
  ): Task[StorageQueueEvent] = {
    val listener: ProgressListener = progressListener(uploadEventListener)
    val putObjectRequest           = request(localFile, bucket, batchMode, listener)
    transferManager
      .upload(putObjectRequest)
      .map(_.waitForUploadResult)
      .map(upload =>
        UploadQueueEvent(RemoteKey(upload.getKey), MD5Hash(upload.getETag)))
      .catchAll(
        e =>
          Task.succeed(
            ErrorQueueEvent(Action.Upload(localFile.remoteKey.key),
                            localFile.remoteKey,
                            e)))
  }

  private def request(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      listener: ProgressListener
  ): PutObjectRequest = {
    val metadata = new ObjectMetadata()
    localFile.md5base64.foreach(metadata.setContentMD5)
    val request =
      new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
        .withMetadata(metadata)
    if (batchMode) request
    else request.withGeneralProgressListener(listener)
  }

  private def progressListener(uploadEventListener: UploadEventListener) =
    new ProgressListener {
      override def progressChanged(progressEvent: ProgressEvent): Unit =
        uploadEventListener.listener(eventHandler(progressEvent))

      private def eventHandler(progressEvent: ProgressEvent) = {
        progressEvent match {
          case e: ProgressEvent if isTransfer(e) =>
            TransferEvent(e.getEventType.name)
          case e: ProgressEvent if isByteTransfer(e) =>
            ByteTransferEvent(e.getEventType.name)
          case e: ProgressEvent =>
            RequestEvent(e.getEventType.name, e.getBytes, e.getBytesTransferred)
        }
      }
    }

  private def isTransfer(e: ProgressEvent) =
    e.getEventType.isTransferEvent

  private def isByteTransfer(e: ProgressEvent) =
    e.getEventType equals ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT

}

object Uploader extends Uploader
