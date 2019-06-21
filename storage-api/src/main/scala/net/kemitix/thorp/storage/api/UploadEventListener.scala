package net.kemitix.thorp.storage.api

import net.kemitix.thorp.domain.LocalFile
import net.kemitix.thorp.storage.api.UploadEvent.RequestEvent

class UploadEventListener(localFile: LocalFile)
  extends UploadProgressLogging {

  var bytesTransferred = 0L

  def listener: UploadEvent => Unit =
    {
      case e: RequestEvent =>
        bytesTransferred += e.transferred
        logRequestCycle(localFile, e, bytesTransferred)
      case _ => ()
    }
}
