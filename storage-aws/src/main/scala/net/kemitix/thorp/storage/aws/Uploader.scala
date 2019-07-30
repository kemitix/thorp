package net.kemitix.thorp.storage.aws

import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import net.kemitix.thorp.config.Config
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
import zio.{UIO, ZIO}

trait Uploader {

  def upload(transferManager: => AmazonTransferManager)(
      localFile: LocalFile,
      bucket: Bucket,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): ZIO[Config, Nothing, StorageQueueEvent] =
    transfer(transferManager)(localFile, bucket, uploadEventListener)
      .catchAll(handleError(localFile.remoteKey))

  private def handleError(remoteKey: RemoteKey)(e: Throwable) =
    UIO.succeed(ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, e))

  private def transfer(transferManager: => AmazonTransferManager)(
      localFile: LocalFile,
      bucket: Bucket,
      uploadEventListener: UploadEventListener
  ) = {
    val listener = progressListener(uploadEventListener)
    for {
      putObjectRequest <- request(localFile, bucket, listener)
      event            <- dispatch(transferManager, putObjectRequest)
    } yield event
  }

  private def dispatch(
      transferManager: AmazonTransferManager,
      putObjectRequest: PutObjectRequest
  ) = {
    transferManager
      .upload(putObjectRequest)
      .map(_.waitForUploadResult)
      .map(uploadResult =>
        UploadQueueEvent(RemoteKey(uploadResult.getKey),
                         MD5Hash(uploadResult.getETag)))
  }

  private def request(
      localFile: LocalFile,
      bucket: Bucket,
      listener: ProgressListener
  ) = {
    val request =
      new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
        .withMetadata(metadata(localFile))
    for {
      batchMode <- Config.batchMode
      r = if (batchMode) request
      else request.withGeneralProgressListener(listener)
    } yield r
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
