package net.kemitix.s3thorp.aws.api

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.LocalFile
import net.kemitix.s3thorp.domain.SizeTranslation.sizeInEnglish

import scala.io.AnsiColor._

trait UploadProgressLogging {

  def logTransfer(localFile: LocalFile,
                  event: TransferEvent)
                 (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(2)(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  val clearLine = s"\u001B[2K\r"
  val progressBarLength = 100

  def logRequestCycle(localFile: LocalFile,
                      event: RequestEvent,
                      bytesTransferred: Long)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] = {
    val remoteKey = localFile.remoteKey.key
    val fileLength = localFile.file.length
    val done = ((bytesTransferred.toDouble / fileLength.toDouble) * 100).toInt
    if (done < 100) {
      val head = s"$GREEN_B$GREEN#$RESET" * done
      val tail = " " * (progressBarLength - done)
      val bar = s"[$head$tail]"
      val transferred = sizeInEnglish(bytesTransferred)
      val fileSize = sizeInEnglish(fileLength)
      IO(print(s"$bar $transferred of $fileSize $remoteKey$clearLine"))
    } else
      IO(print(clearLine))
  }

  def logByteTransfer(event: ByteTransferEvent)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(3)(".")

}
