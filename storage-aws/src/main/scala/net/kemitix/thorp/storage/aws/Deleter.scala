package net.kemitix.thorp.storage.aws

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.domain.StorageQueueEvent.DeleteQueueEvent
import net.kemitix.thorp.domain.{Bucket, RemoteKey}

class Deleter(amazonS3: AmazonS3) {

  def delete(bucket: Bucket,
             remoteKey: RemoteKey): IO[DeleteQueueEvent] =
    for {
      _ <- deleteObject(bucket, remoteKey)
    } yield DeleteQueueEvent(remoteKey)

  private def deleteObject(bucket: Bucket, remoteKey: RemoteKey) =
    IO(amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key)))

}
