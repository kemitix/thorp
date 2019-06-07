package net.kemitix.s3thorp.aws.lib

import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import net.kemitix.s3thorp.aws.api.S3Client

object S3ClientBuilder {

  def createClient(amazonS3Client: AmazonS3,
                   amazonS3TransferManager: TransferManager): S3Client =
    new ThorpS3Client(amazonS3Client, amazonS3TransferManager)

  val defaultClient: S3Client =
    createClient(AmazonS3ClientBuilder.defaultClient, TransferManagerBuilder.defaultTransferManager)

}
