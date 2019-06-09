package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.s3thorp.aws.api.S3Action.UploadS3Action
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.aws.api.{S3Action, UploadProgressListener}
import net.kemitix.s3thorp.aws.lib.S3ClientTransferManagerLogging.{logMultiPartUploadFinished, logMultiPartUploadStart}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, MD5Hash, RemoteKey}

class S3ClientTransferManager(transferManager: => TransferManager) {

  def accepts(localFile: LocalFile)
             (implicit multiPartThreshold: Long): Boolean =
    localFile.file.length >= multiPartThreshold

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             multiPartThreshold: Long,
             tryCount: Int,
             maxRetries: Int)
            (implicit info: Int => String => IO[Unit],
             warn: String => IO[Unit]): IO[S3Action] = {
    val putObjectRequest: PutObjectRequest =
      new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
        .withGeneralProgressListener(progressListener(uploadProgressListener))
    for {
      _ <- logMultiPartUploadStart(localFile, tryCount)
      upload = transferManager.upload(putObjectRequest)
      result <- IO{upload.waitForUploadResult}
      _ <- logMultiPartUploadFinished(localFile)
    } yield UploadS3Action(RemoteKey(result.getKey), MD5Hash(result.getETag))
  }

  def progressListener(uploadProgressListener: UploadProgressListener): ProgressListener = {
    event: ProgressEvent => {
      event match {
        case e if e.getEventType.isTransferEvent => TransferEvent(e.getEventType.name)
        case e if e.getEventType equals ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT => ByteTransferEvent(e.getEventType.name)
        case e => RequestEvent(e.getEventType.name, e.getBytes, e.getBytesTransferred)
      }
    }
  }

}
