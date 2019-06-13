package net.kemitix.s3thorp.aws.api

import cats.Monad
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.LocalFile

class UploadProgressListener[M[_]: Monad](localFile: LocalFile)
                                         (implicit info: Int => String => M[Unit])
  extends UploadProgressLogging {

  var bytesTransferred = 0L

  def listener: UploadEvent => M[Unit] =
    {
      case e: TransferEvent => logTransfer(localFile, e)
      case e: RequestEvent => {
        val transferred = e.transferred
        bytesTransferred += transferred
        logRequestCycle(localFile, e, bytesTransferred)
      }
      case e: ByteTransferEvent => logByteTransfer(e)
    }
}
