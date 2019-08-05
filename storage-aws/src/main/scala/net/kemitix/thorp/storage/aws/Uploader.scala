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

  case class Request(
      localFile: LocalFile,
      bucket: Bucket,
      uploadEventListener: UploadEventListener.Settings
  )

  def upload(transferManager: => AmazonTransferManager)(
      request: Request): ZIO[Config, Nothing, StorageQueueEvent] =
    transfer(transferManager)(request)
      .catchAll(handleError(request.localFile.remoteKey))

  private def handleError(remoteKey: RemoteKey)(e: Throwable) =
    UIO(ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, e))

  private def transfer(transferManager: => AmazonTransferManager)(
      request: Request
  ) =
    putObjectRequest(request) >>=
      dispatch(transferManager)

  private def dispatch(transferManager: AmazonTransferManager)(
      putObjectRequest: PutObjectRequest
  ) = {
    transferManager
      .upload(putObjectRequest)
      .map(_.waitForUploadResult)
      .map(uploadResult =>
        UploadQueueEvent(RemoteKey(uploadResult.getKey),
                         MD5Hash(uploadResult.getETag)))
  }

  private def putObjectRequest(
      request: Request
  ) = {
    val putRequest =
      new PutObjectRequest(request.bucket.name,
                           request.localFile.remoteKey.key,
                           request.localFile.file)
        .withMetadata(metadata(request.localFile))
    for {
      batchMode <- Config.batchMode
      r = if (batchMode) putRequest
      else
        putRequest.withGeneralProgressListener(
          progressListener(request.uploadEventListener))
    } yield r
  }

  private def metadata: LocalFile => ObjectMetadata = localFile => {
    val metadata = new ObjectMetadata()
    LocalFile.md5base64(localFile).foreach(metadata.setContentMD5)
    metadata
  }

  private def progressListener
    : UploadEventListener.Settings => ProgressListener =
    listenerSettings =>
      new ProgressListener {
        override def progressChanged(progressEvent: ProgressEvent): Unit =
          UploadEventListener(listenerSettings)(eventHandler(progressEvent))

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
