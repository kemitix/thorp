package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.{
  AmazonS3Exception,
  CopyObjectRequest,
  CopyObjectResult
}
import net.kemitix.thorp.domain.StorageQueueEvent.{Action, CopyQueueEvent}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.aws.S3ClientException.{
  HashMatchError,
  S3Exception
}
import zio.Task

import scala.util.{Failure, Success, Try}

class Copier(amazonS3: AmazonS3.Client) {

  def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): Task[StorageQueueEvent] =
    for {
      copyResult <- copyObject(bucket, sourceKey, hash, targetKey)
      result     <- mapCopyResult(copyResult, sourceKey, targetKey)
    } yield result

  private def mapCopyResult(
      copyResult: Try[CopyObjectResult],
      sourceKey: RemoteKey,
      targetKey: RemoteKey
  ) =
    copyResult match {
      case Success(_) => Task.succeed(CopyQueueEvent(sourceKey, targetKey))
      case Failure(_: NullPointerException) =>
        Task.succeed(
          StorageQueueEvent
            .ErrorQueueEvent(
              Action.Copy(s"${sourceKey.key} => ${targetKey.key}"),
              targetKey,
              HashMatchError))
      case Failure(e: AmazonS3Exception) =>
        Task.succeed(
          StorageQueueEvent.ErrorQueueEvent(
            Action.Copy(s"${sourceKey.key} => ${targetKey.key}"),
            targetKey,
            S3Exception(e.getMessage))
        )
      case Failure(e) => Task.fail(e)
    }

  private def copyObject(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ) = {
    val request =
      new CopyObjectRequest(
        bucket.name,
        sourceKey.key,
        bucket.name,
        targetKey.key
      ).withMatchingETagConstraint(hash.hash)
    Task(Try(amazonS3.copyObject(request)))
  }

}
