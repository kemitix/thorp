package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import net.kemitix.thorp.storage.api.Storage

object S3StorageServiceBuilder {

  def createService(
      amazonS3Client: AmazonS3.Client,
      amazonTransferManager: AmazonTransferManager
  ): Storage.Service =
    new S3Storage(
      amazonS3Client,
      amazonTransferManager
    )

  lazy val defaultStorageService: Storage.Service =
    createService(
      AmazonS3.ClientImpl(AmazonS3ClientBuilder.defaultClient),
      AmazonTransferManager(TransferManagerBuilder.defaultTransferManager)
    )

}
