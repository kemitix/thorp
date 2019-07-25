package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3 => AmazonS3Client}
import net.kemitix.thorp.storage.aws.S3ClientException.{CopyError, HashError}
import zio.{IO, Task, UIO}

import scala.util.Try

object AmazonS3 {

  trait Client {

    def shutdown(): UIO[Unit]

    def deleteObject: DeleteObjectRequest => Task[Unit]

    def copyObject: CopyObjectRequest => IO[S3ClientException, CopyObjectResult]

    def listObjectsV2: ListObjectsV2Request => Task[ListObjectsV2Result]

  }

  case class ClientImpl(amazonS3: AmazonS3Client) extends Client {

    def shutdown(): UIO[Unit] = UIO(amazonS3.shutdown())

    def deleteObject: DeleteObjectRequest => Task[Unit] =
      request => Task(amazonS3.deleteObject(request))

    def copyObject
      : CopyObjectRequest => IO[S3ClientException, CopyObjectResult] =
      request =>
        Try(amazonS3.copyObject(request))
          .fold(handleError, handleResult)

    private def handleResult
      : CopyObjectResult => IO[S3ClientException, CopyObjectResult] =
      result => IO.fromEither(Option(result).toRight(HashError))

    private def handleError
      : Throwable => IO[S3ClientException, CopyObjectResult] =
      error => Task.fail(CopyError(error))

    def listObjectsV2: ListObjectsV2Request => Task[ListObjectsV2Result] =
      request => Task(amazonS3.listObjectsV2(request))

  }

}
