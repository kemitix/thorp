package net.kemitix.thorp.aws.api

import net.kemitix.thorp.aws.api.UploadEvent.RequestEvent
import net.kemitix.thorp.domain.LocalFile

class UploadProgressListener(localFile: LocalFile)
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
