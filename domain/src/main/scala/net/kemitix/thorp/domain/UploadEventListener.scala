package net.kemitix.thorp.domain

import java.util.concurrent.atomic.AtomicLong

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
      val bytesTransferred = new AtomicLong
      uploadEvent match {
        case e: RequestEvent =>
          UploadEventLogger(
            RequestCycle(settings.localFile,
                         bytesTransferred.addAndGet(e.transferred),
                         settings.index,
                         settings.syncTotals,
                         settings.totalBytesSoFar))
        case _ => ()
      }
    }

}
