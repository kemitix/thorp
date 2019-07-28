package net.kemitix.thorp.storage.aws

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{CopyObjectRequest, CopyObjectResult}
import net.kemitix.thorp.domain.StorageQueueEvent.{
  Action,
  CopyQueueEvent,
  ErrorQueueEvent
}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.aws.S3ClientException.{CopyError, HashError}
import zio.{IO, Task, UIO}

trait Copier {

  def copy(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): UIO[StorageQueueEvent] =
    copyObject(amazonS3)(bucket, sourceKey, hash, targetKey)
      .fold(foldFailure(sourceKey, targetKey),
            foldSuccess(sourceKey, targetKey))

  private def copyObject(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): IO[S3ClientException, CopyObjectResult] = {

    def handleResult
      : Option[CopyObjectResult] => IO[S3ClientException, CopyObjectResult] =
      maybeResult =>
        IO.fromEither {
          maybeResult
            .toRight(HashError)
      }

    def handleError: Throwable => IO[S3ClientException, CopyObjectResult] =
      error =>
        Task.fail {
          CopyError(error)
      }

    val request =
      new CopyObjectRequest(
        bucket.name,
        sourceKey.key,
        bucket.name,
        targetKey.key
      ).withMatchingETagConstraint(hash.hash)
    amazonS3
      .copyObject(request)
      .fold(handleError, handleResult)
      .flatten
  }

  private def foldFailure(
      sourceKey: RemoteKey,
      targetKey: RemoteKey): S3ClientException => StorageQueueEvent = {
    case error: SdkClientException =>
      errorEvent(sourceKey, targetKey, error)
    case error =>
      errorEvent(sourceKey, targetKey, error)

  }

  private def foldSuccess(
      sourceKey: RemoteKey,
      targetKey: RemoteKey): CopyObjectResult => StorageQueueEvent =
    result =>
      Option(result) match {
        case Some(_) => CopyQueueEvent(sourceKey, targetKey)
        case None =>
          errorEvent(sourceKey, targetKey, HashError)
    }

  private def errorEvent: (RemoteKey, RemoteKey, Throwable) => ErrorQueueEvent =
    (sourceKey, targetKey, error) =>
      ErrorQueueEvent(action(sourceKey, targetKey), targetKey, error)

  private def action(sourceKey: RemoteKey, targetKey: RemoteKey): Action =
    Action.Copy(s"${sourceKey.key} => ${targetKey.key}")

}

object Copier extends Copier
