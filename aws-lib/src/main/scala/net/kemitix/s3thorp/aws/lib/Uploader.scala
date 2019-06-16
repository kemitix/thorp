package net.kemitix.s3thorp.aws.lib

import cats.Monad
import cats.implicits._
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{TransferManager => AmazonTransferManager}
import net.kemitix.s3thorp.aws.api.S3Action.{ErroredS3Action, UploadS3Action}
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.aws.api.{S3Action, UploadProgressListener}
import net.kemitix.s3thorp.aws.lib.UploaderLogging.{logMultiPartUploadFinished, logMultiPartUploadStart}
import net.kemitix.thorp.domain._

import scala.util.Try

class Uploader[M[_]: Monad](transferManager: => AmazonTransferManager) {

  def accepts(localFile: LocalFile)
             (implicit multiPartThreshold: Long): Boolean =
    localFile.file.length >= multiPartThreshold

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             multiPartThreshold: Long,
             tryCount: Int,
             maxRetries: Int)
            (implicit logger: Logger[M]): M[S3Action] =
    for {
      _ <- logMultiPartUploadStart[M](localFile, tryCount)
      upload <- transfer(localFile, bucket, uploadProgressListener)
      _ <- logMultiPartUploadFinished[M](localFile)
    } yield upload match {
      case Right(r) => UploadS3Action(RemoteKey(r.getKey), MD5Hash(r.getETag))
      case Left(e) => ErroredS3Action(localFile.remoteKey, e)
    }

  private def transfer(localFile: LocalFile,
                       bucket: Bucket,
                       uploadProgressListener: UploadProgressListener,
                      ): M[Either[Throwable, UploadResult]] = {
    val listener: ProgressListener = progressListener(uploadProgressListener)
    val putObjectRequest = request(localFile, bucket, listener)
    Monad[M].pure {
      Try(transferManager.upload(putObjectRequest))
        .map(_.waitForUploadResult)
        .toEither
    }
  }

  private def request(localFile: LocalFile, bucket: Bucket, listener: ProgressListener): PutObjectRequest =
    new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
      .withGeneralProgressListener(listener)

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
