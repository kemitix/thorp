package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.aws.api.{S3Action, UploadProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile}

trait S3ClientUploader {

  def accepts(localFile: LocalFile)
             (implicit multiPartThreshold: Long): Boolean

  def upload(localFile: LocalFile,
             bucket: Bucket,
             progressListener: UploadProgressListener,
             multiPartThreshold: Long,
             tryCount: Int,
             maxRetries: Int)
            (implicit info: Int => String => Unit,
             warn: String => Unit): IO[S3Action]

  def progressListener(uploadProgressListener: UploadProgressListener): ProgressListener = {
    new ProgressListener {
      override def progressChanged(event: ProgressEvent): Unit = {
        event match {
          case e if e.getEventType.isTransferEvent => TransferEvent(e.getEventType.name)
          case e if e.getEventType equals ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT => ByteTransferEvent(e.getEventType.name)
          case e => RequestEvent(e.getEventType.name, e.getBytes, e.getBytesTransferred)
        }
      }
    }
  }

}
