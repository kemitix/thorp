package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult

object AmazonUpload {

  trait InProgress {
    def waitForUploadResult: UploadResult
  }

  final case class CompletableUpload(upload: Upload) extends InProgress {
    override def waitForUploadResult: UploadResult =
      upload.waitForUploadResult()
  }

}
