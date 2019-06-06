package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.awssdk.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.{Config, LocalFile}

class UploadProgressListener(localFile: LocalFile)
  (implicit info: Int => String => Unit)
  extends UploadProgressLogging {

  def listener: UploadEvent => Unit =
    {
      case e: TransferEvent => logTransfer(localFile, e)
      case e: RequestEvent => logRequestCycle(localFile, e)
      case e: ByteTransferEvent => logByteTransfer(e)
    }
}
