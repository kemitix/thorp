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
      .catchAll(handleError(localFile.remoteKey))

  private def handleError(
      remoteKey: RemoteKey): Throwable => UIO[ErrorQueueEvent] = { e =>
    UIO.succeed(ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, e))
  }

  private def transfer(transferManager: => AmazonTransferManager)(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener
  ): Task[StorageQueueEvent] =
    transferManager
      .upload(
        request(localFile,
                bucket,
                batchMode,
                progressListener(uploadEventListener)))
      .map(_.waitForUploadResult)
      .map(upload =>
        UploadQueueEvent(RemoteKey(upload.getKey), MD5Hash(upload.getETag)))

  private def request(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      listener: ProgressListener
  ): PutObjectRequest = {
    val request =
      new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
        .withMetadata(metadata(localFile))
    if (batchMode) request
    else request.withGeneralProgressListener(listener)
  }

  private def metadata: LocalFile => ObjectMetadata = localFile => {
    val metadata = new ObjectMetadata()
    localFile.md5base64.foreach(metadata.setContentMD5)
    metadata
  }

  private def progressListener: UploadEventListener => ProgressListener =
    uploadEventListener =>
      new ProgressListener {
        override def progressChanged(progressEvent: ProgressEvent): Unit =
          uploadEventListener.listener(eventHandler(progressEvent))

        private def eventHandler: ProgressEvent => UploadEvent =
          progressEvent => {
            def isTransfer: ProgressEvent => Boolean =
              _.getEventType.isTransferEvent
            def isByteTransfer: ProgressEvent => Boolean =
              _.getEventType.equals(
                ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT)
            progressEvent match {
              case e: ProgressEvent if isTransfer(e) =>
                TransferEvent(e.getEventType.name)
              case e: ProgressEvent if isByteTransfer(e) =>
                ByteTransferEvent(e.getEventType.name)
              case e: ProgressEvent =>
                RequestEvent(e.getEventType.name,
                             e.getBytes,
                             e.getBytesTransferred)
            }
          }
    }

}

object Uploader extends Uploader
