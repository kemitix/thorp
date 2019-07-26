package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.domain.StorageQueueEvent.{
  Action,
  DeleteQueueEvent,
  ErrorQueueEvent
}
import net.kemitix.thorp.domain.{Bucket, RemoteKey, StorageQueueEvent}
import zio.{Task, UIO}

trait Deleter {

  def delete(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): UIO[StorageQueueEvent] =
    deleteObject(amazonS3)(bucket, remoteKey)
      .map(_ => DeleteQueueEvent(remoteKey))
      .catchAll(e =>
        UIO(ErrorQueueEvent(Action.Delete(remoteKey.key), remoteKey, e)))

  private def deleteObject(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): Task[Unit] =
    amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key))

}

object Deleter extends Deleter
