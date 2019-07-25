package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.domain.StorageQueueEvent.DeleteQueueEvent
import net.kemitix.thorp.domain.{Bucket, RemoteKey, StorageQueueEvent}
import zio.Task

class Deleter(amazonS3: AmazonS3.Client) {

  def delete(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): Task[StorageQueueEvent] =
    for {
      _ <- deleteObject(bucket, remoteKey)
    } yield DeleteQueueEvent(remoteKey)

  private def deleteObject(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): Task[Unit] =
    Task {
      amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key))
    }

}
