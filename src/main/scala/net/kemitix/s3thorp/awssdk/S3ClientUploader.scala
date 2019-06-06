package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, Config, LocalFile}
import net.kemitix.s3thorp.S3Action
import net.kemitix.s3thorp.awssdk.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}

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
          TransferEvent(event.getEventType.name)
        else if (event.getEventType equals ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT)
          ByteTransferEvent(event.getEventType.name)
        else
          RequestEvent(event.getEventType.name, event.getBytes, event.getBytesTransferred)
      }
    }
  }

}
