package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult

case class AmazonUpload(upload: Upload) {
  def waitForUploadResult: UploadResult = upload.waitForUploadResult()
}
