package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.thorp.storage.aws.AmazonUpload.{
  CompletableUpload,
  InProgress
}
import zio.{Task, UIO}

trait AmazonTransferManager {
  def shutdownNow(now: Boolean): UIO[Unit]
  def upload: PutObjectRequest => Task[InProgress]
}

object AmazonTransferManager {

  final case class Wrapper(transferManager: TransferManager)
      extends AmazonTransferManager {
    def shutdownNow(now: Boolean): UIO[Unit] =
      UIO(transferManager.shutdownNow(now))

    def upload: PutObjectRequest => Task[InProgress] =
      putObjectRequest =>
        Task(transferManager.upload(putObjectRequest))
          .map(CompletableUpload)

  }

}
