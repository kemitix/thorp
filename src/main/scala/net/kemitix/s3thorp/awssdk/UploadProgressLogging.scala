package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.domain.{Config, LocalFile}
import net.kemitix.s3thorp.Logging
import net.kemitix.s3thorp.awssdk.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}

trait UploadProgressLogging
      extends Logging {

  def logTransfer(localFile: LocalFile,
                  event: TransferEvent)
                 (implicit c: Config): Unit =
      log2(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  def logRequestCycle(localFile: LocalFile,
                      event: RequestEvent)
                     (implicit c: Config): Unit =
    log3(s"Uploading:${event.name}:${event.transferred}/${event.bytes}:${localFile.remoteKey.key}")

  def logByteTransfer(event: ByteTransferEvent)
                     (implicit c: Config): Unit =
    log3(".")

}
