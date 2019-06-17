package net.kemitix.thorp.aws.lib

import cats.Monad
import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import net.kemitix.s3thorp.aws.api.S3Client

object S3ClientBuilder {

  def createClient[M[_]: Monad](amazonS3Client: AmazonS3,
                   amazonS3TransferManager: TransferManager): S3Client[M] =
    new ThorpS3Client(amazonS3Client, amazonS3TransferManager)

  def defaultClient[M[_]: Monad]: S3Client[M] =
    createClient(AmazonS3ClientBuilder.defaultClient, TransferManagerBuilder.defaultTransferManager)

}
