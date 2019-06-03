package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile}
import net.kemitix.s3thorp.{Config, S3Action}

trait S3ClientUploader {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean

  def upload(localFile: LocalFile,
             bucket: Bucket,
             progressListener: UploadProgressListener,
             tryCount: Int)
            (implicit c: Config): IO[S3Action]



  def progressListener(uploadProgressListener: UploadProgressListener): ProgressListener = {
    new ProgressListener {
      override def progressChanged(event: ProgressEvent): Unit = {
        if (event.getEventType.isTransferEvent)
          UploadTransferEvent(event.getEventType.name)
        else if (event.getEventType equals ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT)
          UploadByteTransferEvent(event.getEventType.name)
        else
          UploadRequestEvent(event.getEventType.name, event.getBytes, event.getBytesTransferred)
      }
    }
  }

}
