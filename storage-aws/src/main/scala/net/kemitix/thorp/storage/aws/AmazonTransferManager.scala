package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.{TransferManager, Upload}

case class AmazonTransferManager(transferManager: TransferManager) {
  def shutdownNow(now: Boolean): Unit = transferManager.shutdownNow(now)

  def upload(putObjectRequest: PutObjectRequest): Upload =
    transferManager.upload(putObjectRequest)
}
