package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.UploadEvent.RequestEvent
import net.kemitix.thorp.domain.UploadEventLogger.logRequestCycle

class UploadEventListener(localFile: LocalFile) {

  var bytesTransferred = 0L

  def listener: UploadEvent => Unit =
    {
      case e: RequestEvent =>
        bytesTransferred += e.transferred
        logRequestCycle(localFile, e, bytesTransferred)
      case _ => ()
    }
}
