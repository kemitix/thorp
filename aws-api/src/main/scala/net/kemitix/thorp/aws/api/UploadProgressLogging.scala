package net.kemitix.thorp.aws.api

import net.kemitix.thorp.aws.api.UploadEvent.RequestEvent
import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.domain.{LocalFile, Terminal}

trait UploadProgressLogging {

  private val oneHundredPercent = 100

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
