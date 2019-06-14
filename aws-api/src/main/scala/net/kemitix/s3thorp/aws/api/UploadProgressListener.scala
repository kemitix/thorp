package net.kemitix.s3thorp.aws.api

import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.LocalFile

class UploadProgressListener(localFile: LocalFile)
  extends UploadProgressLogging {

  var bytesTransferred = 0L

  def listener: UploadEvent => Unit =
    {
      case e: RequestEvent =>
        bytesTransferred += e.transferred
        logRequestCycle(localFile, e, bytesTransferred)
      case _ => ()
    }
}
