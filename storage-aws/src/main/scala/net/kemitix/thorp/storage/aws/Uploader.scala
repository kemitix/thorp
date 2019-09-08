package net.kemitix.thorp.storage.aws

import java.util.concurrent.locks.StampedLock

import com.amazonaws.event.ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT
import com.amazonaws.event.{ProgressEvent, ProgressListener}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import net.kemitix.thorp.domain.Implicits._
import net.kemitix.thorp.domain.StorageEvent.{
  ActionSummary,
  ErrorEvent,
  UploadEvent
}
import net.kemitix.thorp.domain.UploadProgressEvent.{
  ByteTransferEvent,
  RequestEvent,
  TransferEvent
}
import net.kemitix.thorp.domain.{
  Bucket,
  LocalFile,
  MD5Hash,
  RemoteKey,
  StorageEvent,
  UploadEventListener,
  UploadProgressEvent
}
import net.kemitix.thorp.storage.aws.Uploader.Request
import zio.UIO

trait Uploader {

  def upload(transferManager: => AmazonTransferManager)(
      request: Request): UIO[StorageEvent] =
    transfer(transferManager)(request)
      .catchAll(handleError(request.localFile.remoteKey))

  private def handleError(remoteKey: RemoteKey)(
      e: Throwable): UIO[StorageEvent] =
    UIO(ErrorEvent(ActionSummary.Upload(remoteKey.key), remoteKey, e))

  private def transfer(transferManager: => AmazonTransferManager)(
      request: Request
  ) =
    dispatch(transferManager)(putObjectRequest(request))

  private def dispatch(transferManager: AmazonTransferManager)(
      putObjectRequest: PutObjectRequest
  ) = {
    transferManager
      .upload(putObjectRequest)
      .map(_.waitForUploadResult)
      .map(uploadResult =>
        UploadEvent(RemoteKey(uploadResult.getKey),
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
    if (request.uploadEventListener.batchMode) putRequest
    else
      putRequest.withGeneralProgressListener(
        progressListener(request.uploadEventListener))
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
        private val listener = UploadEventListener.listener(listenerSettings)
        private val lock     = new StampedLock
        override def progressChanged(progressEvent: ProgressEvent): Unit = {
          val writeLock = lock.writeLock()
          listener(eventHandler(progressEvent))
          lock.unlock(writeLock)
        }

        private def eventHandler: ProgressEvent => UploadProgressEvent =
          progressEvent => {
            def isTransfer: ProgressEvent => Boolean =
              _.getEventType.isTransferEvent
            def isByteTransfer: ProgressEvent => Boolean =
              (_.getEventType === RESPONSE_BYTE_TRANSFER_EVENT)
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

object Uploader extends Uploader {
  final case class Request(
      localFile: LocalFile,
      bucket: Bucket,
      uploadEventListener: UploadEventListener.Settings
  )
}
