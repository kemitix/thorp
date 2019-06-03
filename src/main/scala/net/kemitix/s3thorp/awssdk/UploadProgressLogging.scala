package net.kemitix.s3thorp.awssdk

import com.amazonaws.event.ProgressEventType
import net.kemitix.s3thorp.domain.LocalFile
import net.kemitix.s3thorp.{Config, Logging}

trait UploadProgressLogging
      extends Logging {

    def logTransfer(localFile: LocalFile,
      eventType: ProgressEventType)
      (implicit c: Config): Unit =
      log2(s"Transfer:${eventType.name}: ${localFile.remoteKey.key}")

    def logRequestCycle(localFile: LocalFile,
      eventType: ProgressEventType,
      bytes: Long,
      transferred: Long)
      (implicit c: Config): Unit =
      if (eventType equals ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) print('.')
      else log3(s"Uploading:${eventType.name}:$transferred/$bytes:${localFile.remoteKey.key}")

  }
