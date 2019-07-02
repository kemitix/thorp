package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import net.kemitix.thorp.storage.api.StorageService

object S3StorageServiceBuilder {

  def createService(amazonS3Client: AmazonS3,
                    amazonS3TransferManager: TransferManager): StorageService =
    new S3StorageService(amazonS3Client, amazonS3TransferManager)

  def defaultStorageService: StorageService =
    createService(
      AmazonS3ClientBuilder.defaultClient,
      TransferManagerBuilder.defaultTransferManager)

}
