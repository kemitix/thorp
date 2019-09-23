package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult
import zio.Task

object AmazonUpload {

  sealed trait InProgress {
    def waitForUploadResult: Task[UploadResult]
  }

  object InProgress {

    final case class Errored(e: Throwable) extends InProgress {
      override def waitForUploadResult: Task[UploadResult] =
        Task.fail(e)
    }

    final case class CompletableUpload(upload: Upload) extends InProgress {
      override def waitForUploadResult: Task[UploadResult] =
        Task(upload.waitForUploadResult())
    }

  }

}
