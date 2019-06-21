package net.kemitix.thorp.aws.lib

import cats.effect.IO
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{TransferManager => AmazonTransferManager}
import net.kemitix.thorp.aws.lib.UploaderLogging.{logMultiPartUploadFinished, logMultiPartUploadStart}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.S3Action.{ErroredS3Action, UploadS3Action}
import net.kemitix.thorp.storage.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.thorp.storage.api.{S3Action, UploadProgressListener}

import scala.util.Try

class Uploader(transferManager: => AmazonTransferManager) {

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             tryCount: Int)
            (implicit logger: Logger): IO[S3Action] =
    for {
      _ <- logMultiPartUploadStart(localFile, tryCount)
      upload <- transfer(localFile, bucket, uploadProgressListener)
      action = upload match {
        case Right(r) => UploadS3Action(RemoteKey(r.getKey), MD5Hash(r.getETag))
        case Left(e) => ErroredS3Action(localFile.remoteKey, e)
      }
      _ <- logMultiPartUploadFinished(localFile)
    } yield action

  private def transfer(localFile: LocalFile,
                       bucket: Bucket,
                       uploadProgressListener: UploadProgressListener,
                      ): IO[Either[Throwable, UploadResult]] = {
    val listener: ProgressListener = progressListener(uploadProgressListener)
    val putObjectRequest = request(localFile, bucket, listener)
    IO {
      Try(transferManager.upload(putObjectRequest))
        .map(_.waitForUploadResult)
        .toEither
    }
  }

  private def request(localFile: LocalFile, bucket: Bucket, listener: ProgressListener): PutObjectRequest = {
    val metadata = new ObjectMetadata()
    localFile.hash.hash64.foreach(metadata.setContentMD5)
    new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
      .withMetadata(metadata)
      .withGeneralProgressListener(listener)
  }

  private def progressListener(uploadProgressListener: UploadProgressListener) =
    new ProgressListener {
      override def progressChanged(progressEvent: ProgressEvent): Unit = {
        uploadProgressListener.listener(eventHandler(progressEvent))
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
