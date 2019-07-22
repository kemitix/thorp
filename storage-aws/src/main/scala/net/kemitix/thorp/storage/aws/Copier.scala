package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.CopyObjectRequest
import net.kemitix.thorp.domain.StorageQueueEvent.CopyQueueEvent
import net.kemitix.thorp.domain._
import zio.Task

class Copier(amazonS3: AmazonS3.Client) {

  def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): Task[StorageQueueEvent] =
    for {
      _      <- copyObject(bucket, sourceKey, hash, targetKey)
      result <- Task.succeed(CopyQueueEvent(targetKey))
    } yield result

  private def copyObject(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ) = {
    val request =
      new CopyObjectRequest(
        bucket.name,
        sourceKey.key,
        bucket.name,
        targetKey.key
      ).withMatchingETagConstraint(hash.hash)
    Task(amazonS3.copyObject(request))
  }

}
