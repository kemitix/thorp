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
      request: Request): UIO[StorageQueueEvent] =
    copyObject(amazonS3)(request)
      .fold(foldFailure(request.sourceKey, request.targetKey),
            foldSuccess(request.sourceKey, request.targetKey))

  case class Request(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  )

  private def copyObject(amazonS3: AmazonS3.Client)(request: Request) =
    amazonS3
      .copyObject(copyObjectRequest(request))
      .fold(
        error => Task.fail(CopyError(error)),
        result => IO.fromEither(result.toRight(HashError))
      )
      .flatten

  private def copyObjectRequest(copyRequest: Request) =
    new CopyObjectRequest(
      copyRequest.bucket.name,
      copyRequest.sourceKey.key,
      copyRequest.bucket.name,
      copyRequest.targetKey.key
    ).withMatchingETagConstraint(MD5Hash.hash(copyRequest.hash))

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
