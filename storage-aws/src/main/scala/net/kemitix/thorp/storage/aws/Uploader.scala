package net.kemitix.thorp.storage.aws

import cats.effect.IO
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{TransferManager => AmazonTransferManager}
import net.kemitix.thorp.domain.StorageQueueEvent.{ErrorQueueEvent, UploadQueueEvent}
import net.kemitix.thorp.domain.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.thorp.domain.{StorageQueueEvent, _}

import scala.util.Try

class Uploader(transferManager: => AmazonTransferManager) {

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadEventListener: UploadEventListener,
             tryCount: Int): IO[StorageQueueEvent] =
    for {
      upload <- transfer(localFile, bucket, uploadEventListener)
      action = upload match {
        case Right(r) => UploadQueueEvent(RemoteKey(r.getKey), MD5Hash(r.getETag))
        case Left(e) => ErrorQueueEvent(localFile.remoteKey, e)
      }
    } yield action

  private def transfer(localFile: LocalFile,
                       bucket: Bucket,
                       uploadEventListener: UploadEventListener,
                      ): IO[Either[Throwable, UploadResult]] = {
    val listener: ProgressListener = progressListener(uploadEventListener)
    val putObjectRequest = request(localFile, bucket, listener)
    IO {
      Try(transferManager.upload(putObjectRequest))
        .map(_.waitForUploadResult)
        .toEither
    }
  }

  private def request(localFile: LocalFile, bucket: Bucket, listener: ProgressListener): PutObjectRequest = {
    val metadata = new ObjectMetadata()
    localFile.md5base64.foreach(metadata.setContentMD5)
    new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
      .withMetadata(metadata)
      .withGeneralProgressListener(listener)
  }

  private def progressListener(uploadEventListener: UploadEventListener) =
    new ProgressListener {
      override def progressChanged(progressEvent: ProgressEvent): Unit = {
        uploadEventListener.listener(eventHandler(progressEvent))
      }

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
