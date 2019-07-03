package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.domain.UploadEvent.RequestEvent

import scala.io.AnsiColor._

trait UploadEventLogger {

  def logRequestCycle(localFile: LocalFile,
                      event: RequestEvent,
                      bytesTransferred: Long,
                      index: Int,
                      syncTotals: SyncTotals): Unit = {
    val remoteKey = localFile.remoteKey.key
    val fileLength = localFile.file.length
    if (bytesTransferred < fileLength) {
      println(
        s"${GREEN}Uploading:$RESET $remoteKey$eraseLineForward\n" +
          statusWithBar(" File", sizeInEnglish, bytesTransferred, fileLength) +
          statusWithBar("Files", l => l.toString, index, syncTotals.count) +
          statusWithBar(" Size", sizeInEnglish, syncTotals.sizeUploadedBytes + bytesTransferred, syncTotals.totalSizeBytes) +
          s"${Terminal.cursorPrevLine() * 7}")
    } else
      println(s"${GREEN}Uploaded:$RESET $remoteKey$eraseLineForward")
  }

  private def statusWithBar(label: String,
                            format: Long => String,
                            current: Long,
                            max: Long,
                            pre: Long = 0): String = {
    s"$GREEN$label:$RESET ${format(current)} of ${format(max)}" +
      (if (pre > 0) s" (pre-synced ${format(pre)}"
      else "") + s"$eraseLineForward\n" +
      progressBar(current, max, Terminal.width)
  }
}

object UploadEventLogger extends UploadEventLogger
