package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import net.kemitix.thorp.storage.api.StorageService

object S3StorageServiceBuilder {

  def createService(
      amazonS3Client: AmazonS3,
      amazonTransferManager: AmazonTransferManager
  ): StorageService =
    new S3StorageService(
      amazonS3Client,
      amazonTransferManager
    )

  lazy val defaultStorageService: StorageService =
    createService(
      AmazonS3ClientBuilder.defaultClient,
      AmazonTransferManager(TransferManagerBuilder.defaultTransferManager)
    )

}
