package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import zio.{Task,UIO}

case class AmazonTransferManager(transferManager: TransferManager) {
  def shutdownNow(now: Boolean): UIO[Unit] =
    UIO(transferManager.shutdownNow(now))

  def upload(putObjectRequest: PutObjectRequest): Task[AmazonUpload] =
    Task(AmazonUpload(transferManager.upload(putObjectRequest)))
}
