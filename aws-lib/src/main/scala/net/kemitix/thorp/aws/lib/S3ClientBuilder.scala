package net.kemitix.thorp.aws.lib

import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import net.kemitix.thorp.storage.api.S3Client

object S3ClientBuilder {

  def createClient(amazonS3Client: AmazonS3,
                   amazonS3TransferManager: TransferManager): S3Client =
    new ThorpS3Client(amazonS3Client, amazonS3TransferManager)

  def defaultClient: S3Client =
    createClient(AmazonS3ClientBuilder.defaultClient, TransferManagerBuilder.defaultTransferManager)

}
