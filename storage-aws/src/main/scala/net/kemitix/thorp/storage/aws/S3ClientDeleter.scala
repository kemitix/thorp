package net.kemitix.thorp.storage.aws

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.domain.StorageQueueEvent.DeleteQueueEvent
import net.kemitix.thorp.domain.{Bucket, Logger, RemoteKey}
import net.kemitix.thorp.storage.aws.S3ClientLogging.{logDeleteStart, logDeleteFinish}

class S3ClientDeleter(amazonS3: AmazonS3) {

  def delete(bucket: Bucket,
             remoteKey: RemoteKey)
            (implicit logger: Logger): IO[DeleteQueueEvent] =
    for {
      _ <- logDeleteStart(bucket, remoteKey)
      _ <- deleteObject(bucket, remoteKey)
      _ <- logDeleteFinish(bucket, remoteKey)
    } yield DeleteQueueEvent(remoteKey)

  private def deleteObject(bucket: Bucket, remoteKey: RemoteKey) =
    IO(amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key)))

}
