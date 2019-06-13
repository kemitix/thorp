package net.kemitix.s3thorp.aws.api

import cats.Monad
import cats.effect.IO
import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.Terminal.{clearLine, returnToPreviousLine}
import net.kemitix.s3thorp.domain.{LocalFile, Terminal}
import net.kemitix.s3thorp.domain.SizeTranslation.sizeInEnglish

import scala.io.AnsiColor._

trait UploadProgressLogging {

  def logTransfer[M[_]: Monad](localFile: LocalFile,
                  event: TransferEvent)
                 (implicit info: Int => String => M[Unit]): M[Unit] =
    info(2)(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  private val oneHundredPercent = 100

  def logRequestCycle[M[_]: Monad](localFile: LocalFile,
                                   event: RequestEvent,
                                   bytesTransferred: Long)
                                  (implicit info: Int => String => M[Unit]): M[Unit] = {
    val remoteKey = localFile.remoteKey.key
    val fileLength = localFile.file.length
    val consoleWidth = Terminal.width - 2
    val done = ((bytesTransferred.toDouble / fileLength.toDouble) * consoleWidth).toInt
    if (done < oneHundredPercent) {
      val head = s"$GREEN_B$GREEN#$RESET" * done
      val tail = " " * (consoleWidth - done)
      val bar = s"[$head$tail]"
      val transferred = sizeInEnglish(bytesTransferred)
      val fileSize = sizeInEnglish(fileLength)
      Monad[M].pure(print(s"${clearLine}Uploading $transferred of $fileSize : $remoteKey\n$bar$returnToPreviousLine"))
    } else
      Monad[M].pure(print(clearLine))
  }

  def logByteTransfer(event: ByteTransferEvent)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(3)(".")

}
