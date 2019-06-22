package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.domain.UploadEvent.RequestEvent

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
      print(s"${eraseLine}Uploading $transferred of $fileSize : $remoteKey\n$bar${cursorUp()}\r")
    } else
      print(eraseLine)
  }

}

object UploadEventLogger extends UploadEventLogger