package net.kemitix.thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import net.kemitix.thorp.aws.lib.S3ClientLogging.{logCopyFinish, logCopyStart}
import net.kemitix.thorp.domain.{Bucket, Logger, MD5Hash, RemoteKey}
import net.kemitix.thorp.storage.api.StorageQueueEvent
import net.kemitix.thorp.storage.api.StorageQueueEvent.CopyQueueEvent

class S3ClientCopier(amazonS3: AmazonS3) {

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey)
          (implicit logger: Logger): IO[StorageQueueEvent] =
    for {
      _ <- logCopyStart(bucket, sourceKey, targetKey)
      _ <- copyObject(bucket, sourceKey, hash, targetKey)
      _ <- logCopyFinish(bucket, sourceKey,targetKey)
    } yield CopyQueueEvent(targetKey)

  private def copyObject(bucket: Bucket,
                         sourceKey: RemoteKey,
                         hash: MD5Hash,
                         targetKey: RemoteKey) = {
    val request =
      new CopyObjectRequest(bucket.name, sourceKey.key, bucket.name, targetKey.key)
        .withMatchingETagConstraint(hash.hash)
    IO(amazonS3.copyObject(request))
  }

}
