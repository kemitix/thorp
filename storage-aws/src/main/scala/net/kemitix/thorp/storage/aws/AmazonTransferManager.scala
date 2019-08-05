package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.thorp.storage.aws.AmazonUpload.{
  CompletableUpload,
  InProgress
}
import zio.{Task, UIO}

final case class AmazonTransferManager(transferManager: TransferManager) {
  def shutdownNow(now: Boolean): UIO[Unit] =
    UIO(transferManager.shutdownNow(now))

  def upload: PutObjectRequest => Task[InProgress] =
    putObjectRequest =>
      Task(transferManager.upload(putObjectRequest))
        .map(CompletableUpload)

}
