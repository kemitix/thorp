package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.{AmazonS3 => AmazonS3Client}
import com.amazonaws.services.s3.model.{
  CopyObjectRequest,
  CopyObjectResult,
  DeleteObjectRequest,
  ListObjectsV2Request,
  ListObjectsV2Result
}

object AmazonS3 {

  trait Client {

    def shutdown(): Unit

    def deleteObject: DeleteObjectRequest => Unit

    def copyObject: CopyObjectRequest => Option[CopyObjectResult]

    def listObjectsV2: ListObjectsV2Request => ListObjectsV2Result

  }

  case class ClientImpl(amazonS3: AmazonS3Client) extends Client {

    def shutdown(): Unit = amazonS3.shutdown()

    def deleteObject: DeleteObjectRequest => Unit =
      request => amazonS3.deleteObject(request)

    def copyObject: CopyObjectRequest => Option[CopyObjectResult] =
      request => Option(amazonS3.copyObject(request))

    def listObjectsV2: ListObjectsV2Request => ListObjectsV2Result =
      request => amazonS3.listObjectsV2(request)

  }

}
