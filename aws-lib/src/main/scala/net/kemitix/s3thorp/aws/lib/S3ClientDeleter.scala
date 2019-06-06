package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.aws.api.S3Action.DeleteS3Action
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logDeleteFinish, logDeleteStart}
import net.kemitix.s3thorp.domain.{Bucket, RemoteKey}
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

class S3ClientDeleter(s3Client: S3CatsIOClient) {

  def delete(bucket: Bucket,
             remoteKey: RemoteKey)
            (implicit info: Int => String => Unit): IO[DeleteS3Action] = {
    val request = DeleteObjectRequest.builder
      .bucket(bucket.name)
      .key(remoteKey.key).build
    s3Client.deleteObject(request)
      .bracket(
        logDeleteStart(bucket, remoteKey))(
        logDeleteFinish(bucket, remoteKey))
      .map(_ => DeleteS3Action(remoteKey))
  }

}
