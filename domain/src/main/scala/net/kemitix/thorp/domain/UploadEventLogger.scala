package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.domain.UploadEvent.RequestEvent

import scala.io.AnsiColor._

trait UploadEventLogger {

  def logRequestCycle(
      localFile: LocalFile,
      event: RequestEvent,
      bytesTransferred: Long,
      index: Int,
      syncTotals: SyncTotals,
      totalBytesSoFar: Long
  ): Unit = {
    val remoteKey    = localFile.remoteKey.key
    val fileLength   = localFile.file.length
    val statusHeight = 7
    if (bytesTransferred < fileLength)
      println(
        s"${GREEN}Uploading:$RESET $remoteKey$eraseToEndOfScreen\n" +
          statusWithBar(" File", sizeInEnglish, bytesTransferred, fileLength) +
          statusWithBar("Files", l => l.toString, index, syncTotals.count) +
          statusWithBar(" Size",
                        sizeInEnglish,
                        bytesTransferred + totalBytesSoFar,
                        syncTotals.totalSizeBytes) +
          s"${Terminal.cursorPrevLine(statusHeight)}")
  }

  private def statusWithBar(
      label: String,
      format: Long => String,
      current: Long,
      max: Long,
      pre: Long = 0
  ): String = {
    val percent = f"${(current * 100) / max}%2d"
    s"$GREEN$label:$RESET ($percent%) ${format(current)} of ${format(max)}" +
      (if (pre > 0) s" (pre-synced ${format(pre)}"
       else "") + s"$eraseLineForward\n" +
      progressBar(current, max, Terminal.width)
  }
}

object UploadEventLogger extends UploadEventLogger
