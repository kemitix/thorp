package net.kemitix.thorp.storage.api

import net.kemitix.thorp.domain.LocalFile
import net.kemitix.thorp.storage.api.UploadEvent.RequestEvent
import net.kemitix.thorp.storage.api.UploadEventLogger.logRequestCycle

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
