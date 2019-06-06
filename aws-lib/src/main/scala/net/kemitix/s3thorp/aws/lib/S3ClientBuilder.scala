package net.kemitix.s3thorp.aws.lib

import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.aws.api.S3Client

object S3ClientBuilder {

  def createClient(s3AsyncClient: S3AsyncClient,
                   amazonS3Client: AmazonS3,
                   amazonS3TransferManager: TransferManager): S3Client = {
    new ThorpS3Client(S3CatsIOClient(s3AsyncClient), amazonS3Client, amazonS3TransferManager)
  }

  val defaultClient: S3Client =
    createClient(new JavaClientWrapper {}.underlying,
      AmazonS3ClientBuilder.defaultClient,
      TransferManagerBuilder.defaultTransferManager)

}
