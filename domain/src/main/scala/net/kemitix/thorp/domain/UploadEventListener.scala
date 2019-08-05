package net.kemitix.thorp.domain

import java.util.concurrent.atomic.AtomicLong

import net.kemitix.thorp.domain.UploadEvent.RequestEvent
import net.kemitix.thorp.domain.UploadEventLogger.RequestCycle

object UploadEventListener {

  final case class Settings(
      localFile: LocalFile,
      index: Int,
      syncTotals: SyncTotals,
      totalBytesSoFar: Long
  )

  private val bytesTransferred = new AtomicLong(0L)

  def listener(settings: Settings): UploadEvent => Unit = {
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
