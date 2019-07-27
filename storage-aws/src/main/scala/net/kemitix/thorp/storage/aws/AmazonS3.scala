package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3 => AmazonS3Client}
import zio.{Task, UIO}

object AmazonS3 {

  trait Client {

    def shutdown(): UIO[Unit]

    def deleteObject: DeleteObjectRequest => Task[Unit]

    def copyObject: CopyObjectRequest => Task[Option[CopyObjectResult]]

    def listObjectsV2: ListObjectsV2Request => Task[ListObjectsV2Result]

  }

  case class ClientImpl(amazonS3: AmazonS3Client) extends Client {

    def shutdown(): UIO[Unit] =
      UIO {
        amazonS3.shutdown()
      }

    def deleteObject: DeleteObjectRequest => Task[Unit] =
      request =>
        Task {
          amazonS3.deleteObject(request)
      }

    def copyObject: CopyObjectRequest => Task[Option[CopyObjectResult]] =
      request =>
        Task {
          amazonS3.copyObject(request)
        }.map(Option(_))

    def listObjectsV2: ListObjectsV2Request => Task[ListObjectsV2Result] =
      request =>
        Task {
          amazonS3.listObjectsV2(request)
      }

  }

}
