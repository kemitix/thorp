package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.domain.StorageEvent.{
  ActionSummary,
  DeleteEvent,
  ErrorEvent
}
import net.kemitix.thorp.domain.{Bucket, RemoteKey, StorageEvent}
import zio.{Task, UIO, ZIO}

trait Deleter {

  def delete(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): UIO[StorageEvent] =
    deleteObject(amazonS3)(bucket, remoteKey)
      .catchAll(e =>
        UIO(ErrorEvent(ActionSummary.Delete(remoteKey.key), remoteKey, e)))

  private def deleteObject(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): Task[StorageEvent] =
    (amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key))
      *> ZIO(DeleteEvent(remoteKey)))
}

object Deleter extends Deleter
