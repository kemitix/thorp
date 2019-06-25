package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.domain.UploadEvent.RequestEvent

import scala.io.AnsiColor._

trait UploadEventLogger {

  def logRequestCycle(localFile: LocalFile,
                      event: RequestEvent,
                      bytesTransferred: Long): Unit = {
    val remoteKey = localFile.remoteKey.key
    val fileLength = localFile.file.length
    if (bytesTransferred < fileLength) {
      val bar = progressBar(bytesTransferred, fileLength.toDouble, Terminal.width)
      val transferred = sizeInEnglish(bytesTransferred)
      val fileSize = sizeInEnglish(fileLength)
      val message = s"${GREEN}Uploaded $transferred of $fileSize $RESET: $remoteKey$eraseLineForward"
      println(s"$message\n$bar${Terminal.cursorPrevLine() * 2}")
    } else
      println(s"${GREEN}Uploaded:$RESET $remoteKey$eraseLineForward")
  }

}

object UploadEventLogger extends UploadEventLogger
