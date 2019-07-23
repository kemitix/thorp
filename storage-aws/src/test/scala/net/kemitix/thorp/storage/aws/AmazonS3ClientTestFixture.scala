package net.kemitix.thorp.storage.aws

import org.scalamock.scalatest.MockFactory

trait AmazonS3ClientTestFixture extends MockFactory {

  val fixture: Fixture =
    Fixture(stub[AmazonS3.Client], stub[AmazonTransferManager])

  case class Fixture(
      amazonS3Client: AmazonS3.Client,
      amazonS3TransferManager: AmazonTransferManager,
  ) {
    lazy val storageService: S3StorageService =
      new S3StorageService(amazonS3Client, amazonS3TransferManager)
  }

}
