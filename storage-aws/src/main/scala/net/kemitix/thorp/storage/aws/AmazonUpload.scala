package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult

object AmazonUpload {

  sealed trait InProgress {
    def waitForUploadResult: UploadResult
  }

  object InProgress {
    final case class Errored(e: Throwable) extends InProgress {
      override def waitForUploadResult: UploadResult = new UploadResult
    }
    final case class CompletableUpload(upload: Upload) extends InProgress {
      override def waitForUploadResult: UploadResult =
        upload.waitForUploadResult()
    }

  }
}
