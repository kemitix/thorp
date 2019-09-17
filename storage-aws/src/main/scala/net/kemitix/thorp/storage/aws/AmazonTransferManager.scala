package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.thorp.storage.aws.AmazonUpload.InProgress
import zio.{Task, UIO, ZIO}

trait AmazonTransferManager {
  def shutdownNow(now: Boolean): UIO[Unit]
  def upload: PutObjectRequest => UIO[InProgress]
}

object AmazonTransferManager {

  final case class Wrapper(transferManager: TransferManager)
      extends AmazonTransferManager {
    def shutdownNow(now: Boolean): UIO[Unit] =
      UIO(transferManager.shutdownNow(now))

    def upload: PutObjectRequest => UIO[InProgress] =
      putObjectRequest =>
        transfer(transferManager, putObjectRequest)
          .catchAll(e => UIO(InProgress.Errored(e)))

  }

  private def transfer(transferManager: TransferManager,
                       putObjectRequest: PutObjectRequest): Task[InProgress] =
    ZIO(transferManager.upload(putObjectRequest))
      .map(InProgress.CompletableUpload)
}
