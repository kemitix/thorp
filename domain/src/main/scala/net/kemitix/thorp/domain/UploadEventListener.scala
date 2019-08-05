package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.UploadEvent.RequestEvent
import net.kemitix.thorp.domain.UploadEventLogger.RequestCycle

object UploadEventListener {

  case class Settings(
      localFile: LocalFile,
      index: Int,
      syncTotals: SyncTotals,
      totalBytesSoFar: Long
  )

  def apply(settings: Settings): UploadEvent => Unit =
    uploadEvent => {
      var bytesTransferred = 0L
      uploadEvent match {
        case e: RequestEvent =>
          bytesTransferred += e.transferred
          UploadEventLogger(
            RequestCycle(settings.localFile,
                         bytesTransferred,
                         settings.index,
                         settings.syncTotals,
                         settings.totalBytesSoFar))
        case _ => ()
      }
    }

}
