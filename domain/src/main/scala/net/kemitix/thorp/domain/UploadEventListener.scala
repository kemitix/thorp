package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.UploadEvent.RequestEvent
import net.kemitix.thorp.domain.UploadEventLogger.{
  RequestCycle,
  logRequestCycle
}

object UploadEventListener {

  case class Settings(
      localFile: LocalFile,
      index: Int,
      syncTotals: SyncTotals,
      totalBytesSoFar: Long
  )

  var bytesTransferred = 0L

  def listener(settings: Settings): UploadEvent => Unit = {
    case e: RequestEvent =>
      bytesTransferred += e.transferred
      logRequestCycle(
        RequestCycle(settings.localFile,
                     bytesTransferred,
                     settings.index,
                     settings.syncTotals,
                     settings.totalBytesSoFar))
    case _ => ()
  }

}
