package net.kemitix.thorp.aws.lib

import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import net.kemitix.thorp.storage.api.StorageService

object S3StorageService {

  def createService(amazonS3Client: AmazonS3,
                    amazonS3TransferManager: TransferManager): StorageService =
    new ThorpStorageService(amazonS3Client, amazonS3TransferManager)

  def defaultStorageService: StorageService =
    createService(AmazonS3ClientBuilder.defaultClient, TransferManagerBuilder.defaultTransferManager)

}
