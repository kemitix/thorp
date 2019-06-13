package net.kemitix.s3thorp.aws.api

import net.kemitix.s3thorp.aws.api.UploadEvent.{ByteTransferEvent, RequestEvent, TransferEvent}
import net.kemitix.s3thorp.domain.Terminal.{clearLine, returnToPreviousLine}
import net.kemitix.s3thorp.domain.{LocalFile, Terminal}
import net.kemitix.s3thorp.domain.SizeTranslation.sizeInEnglish

import scala.io.AnsiColor._

trait UploadProgressLogging {

  def logTransfer(localFile: LocalFile,
                  event: TransferEvent): Unit =
    println(s"Transfer:${event.name}: ${localFile.remoteKey.key}")

  private val oneHundredPercent = 100

  def logRequestCycle(localFile: LocalFile,
                                   event: RequestEvent,
                                   bytesTransferred: Long): Unit = {
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
      print(s"${clearLine}Uploading $transferred of $fileSize : $remoteKey\n$bar$returnToPreviousLine")
    } else
      print(clearLine)
  }

  def logByteTransfer(event: ByteTransferEvent): Unit =
    print(".")

}
