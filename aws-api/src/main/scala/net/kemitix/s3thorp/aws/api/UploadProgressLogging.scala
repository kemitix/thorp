package net.kemitix.s3thorp.aws.api

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.s3thorp.domain.{LocalFile, SizeTranslation}

trait UploadProgressLogging {

  def logTransfer(localFile: LocalFile,
                  event: TransferEvent)
                 (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(2)(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  def logRequestCycle(localFile: LocalFile,
                      event: RequestEvent,
                      bytesTransferred: Long)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(3)(s"Uploading:${event.name}:${sizeInEnglish(bytesTransferred)}/${sizeInEnglish(localFile.file.length)}:${localFile.remoteKey.key}")

  def logByteTransfer(event: ByteTransferEvent)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(3)(".")

}
