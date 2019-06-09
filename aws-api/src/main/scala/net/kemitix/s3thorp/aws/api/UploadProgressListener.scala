package net.kemitix.s3thorp.aws.api

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.LocalFile

class UploadProgressListener(localFile: LocalFile)
  (implicit info: Int => String => IO[Unit])
  extends UploadProgressLogging {

  def listener: UploadEvent => IO[Unit] =
    {
      case e: TransferEvent => logTransfer(localFile, e)
      case e: RequestEvent => logRequestCycle(localFile, e)
      case e: ByteTransferEvent => logByteTransfer(e)
    }
}
