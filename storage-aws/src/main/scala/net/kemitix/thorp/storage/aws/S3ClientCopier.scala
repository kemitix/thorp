package net.kemitix.thorp.storage.aws

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import net.kemitix.thorp.domain.StorageQueueEvent.CopyQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.aws.S3ClientLogging.{logCopyStart, logCopyFinish}

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
