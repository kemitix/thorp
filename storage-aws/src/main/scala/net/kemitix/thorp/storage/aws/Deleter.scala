package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.domain.StorageEvent.ActionSummary
import net.kemitix.thorp.domain.{Bucket, RemoteKey, StorageEvent}
import zio.{Task, UIO, ZIO}

trait Deleter {

  def delete(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): UIO[StorageEvent] =
    deleteObject(amazonS3)(bucket, remoteKey)
      .catchAll(
        e =>
          UIO(
            StorageEvent
              .errorEvent(ActionSummary.delete(remoteKey.key), remoteKey, e)))

  private def deleteObject(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): Task[StorageEvent] =
    (amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key))
      *> ZIO(StorageEvent.deleteEvent(remoteKey)))
}

object Deleter extends Deleter
