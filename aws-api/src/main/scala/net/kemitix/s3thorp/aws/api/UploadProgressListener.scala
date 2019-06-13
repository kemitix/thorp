package net.kemitix.s3thorp.aws.api

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.LocalFile

class UploadProgressListener(localFile: LocalFile)
  (implicit info: Int => String => IO[Unit])
  extends UploadProgressLogging {

  var bytesTransferred = 0L

  def listener: UploadEvent => IO[Unit] =
    {
      case e: TransferEvent => logTransfer[IO](localFile, e)
      case e: RequestEvent => {
        val transferred = e.transferred
        bytesTransferred += transferred
        logRequestCycle(localFile, e, bytesTransferred)
      }
      case e: ByteTransferEvent => logByteTransfer(e)
    }
}
