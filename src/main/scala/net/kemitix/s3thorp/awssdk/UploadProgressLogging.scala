package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.domain.LocalFile
import net.kemitix.s3thorp.{Config, Logging}

trait UploadProgressLogging
      extends Logging {

  def logTransfer(localFile: LocalFile,
                  event: UploadTransferEvent)
                 (implicit c: Config): Unit =
      log2(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  def logRequestCycle(localFile: LocalFile,
                      event: UploadRequestEvent)
                     (implicit c: Config): Unit =
    log3(s"Uploading:${event.name}:${event.transferred}/${event.bytes}:${localFile.remoteKey.key}")

  def logByteTransfer(event: UploadByteTransferEvent)
                     (implicit c: Config): Unit =
    log3(".")

}
