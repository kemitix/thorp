package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.Terminal._

import scala.io.AnsiColor._

object UploadEventLogger {

  case class RequestCycle(
      localFile: LocalFile,
      bytesTransferred: Long,
      index: Int,
      syncTotals: SyncTotals,
      totalBytesSoFar: Long
  )

  def logRequestCycle(requestCycle: RequestCycle): Unit = {
    val remoteKey    = requestCycle.localFile.remoteKey.key
    val fileLength   = requestCycle.localFile.file.length
    val statusHeight = 7
    if (requestCycle.bytesTransferred < fileLength)
      println(
        s"${GREEN}Uploading:$RESET $remoteKey$eraseToEndOfScreen\n" +
          statusWithBar(" File",
                        SizeTranslation.sizeInEnglish,
                        requestCycle.bytesTransferred,
                        fileLength) +
          statusWithBar("Files",
                        l => l.toString,
                        requestCycle.index,
                        requestCycle.syncTotals.count) +
          statusWithBar(
            " Size",
            SizeTranslation.sizeInEnglish,
            requestCycle.bytesTransferred + requestCycle.totalBytesSoFar,
            requestCycle.syncTotals.totalSizeBytes) +
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
