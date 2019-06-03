package net.kemitix.s3thorp.awssdk

import com.amazonaws.event.{ProgressEvent, ProgressListener}
import net.kemitix.s3thorp.Config
import net.kemitix.s3thorp.domain.LocalFile

class UploadProgressListener(localFile: LocalFile)
  (implicit c: Config)
  extends UploadProgressLogging {

  def listener: ProgressListener = new ProgressListener {
    override def progressChanged(progressEvent: ProgressEvent): Unit = {
      val eventType = progressEvent.getEventType
      if (eventType.isTransferEvent) logTransfer(localFile, eventType)
      else logRequestCycle(localFile, eventType, progressEvent.getBytes, progressEvent.getBytesTransferred)
    }
  }
}
