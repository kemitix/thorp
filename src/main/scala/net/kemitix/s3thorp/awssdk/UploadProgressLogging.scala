package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.awssdk.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.LocalFile

trait UploadProgressLogging {

  def logTransfer(localFile: LocalFile,
                  event: TransferEvent)
                 (implicit info: Int => String => Unit): Unit =
      info(2)(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  def logRequestCycle(localFile: LocalFile,
                      event: RequestEvent)
                     (implicit info: Int => String => Unit): Unit =
    info(3)(s"Uploading:${event.name}:${event.transferred}/${event.bytes}:${localFile.remoteKey.key}")

  def logByteTransfer(event: ByteTransferEvent)
                     (implicit info: Int => String => Unit): Unit =
    info(3)(".")

}
