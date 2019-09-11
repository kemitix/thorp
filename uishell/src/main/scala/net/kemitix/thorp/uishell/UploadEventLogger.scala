package net.kemitix.thorp.uishell

import net.kemitix.thorp.domain.Terminal.{
  eraseLineForward,
  eraseToEndOfScreen,
  progressBar
}
import net.kemitix.thorp.domain.{LocalFile, SizeTranslation, Terminal}

import scala.io.AnsiColor.{GREEN, RESET}

object UploadEventLogger {

  final case class RequestCycle(
      localFile: LocalFile,
      bytesTransferred: Long,
      index: Int,
      totalBytesSoFar: Long
  )

  def apply(requestCycle: RequestCycle): Unit = {
    val remoteKey    = requestCycle.localFile.remoteKey.key
    val fileLength   = requestCycle.localFile.file.length
    val statusHeight = 3
    if (requestCycle.bytesTransferred < fileLength)
      println(
        s"${GREEN}Uploading:$RESET $remoteKey$eraseToEndOfScreen\n" +
          statusWithBar(" File",
                        SizeTranslation.sizeInEnglish,
                        requestCycle.bytesTransferred,
                        fileLength) +
          s"${Terminal.cursorPrevLine(statusHeight)}")
  }

  private def statusWithBar(
      label: String,
      format: Long => String,
      current: Long,
      max: Long
  ): String = {
    val percent = f"${(current * 100) / max}%2d"
    s"$GREEN$label:$RESET ($percent%) ${format(current)} of ${format(max)}" +
      s"$eraseLineForward\n" +
      progressBar(current, max, Terminal.width)
  }
}
