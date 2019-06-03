package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.Config
import net.kemitix.s3thorp.domain.LocalFile

class UploadProgressListener(localFile: LocalFile)
  (implicit c: Config)
  extends UploadProgressLogging {

  def listener: UploadEvent => Unit =
    {
      case e: UploadTransferEvent => logTransfer(localFile, e)
      case e: UploadRequestEvent => logRequestCycle(localFile, e)
      case e: UploadByteTransferEvent => logByteTransfer(e)
    }
}
