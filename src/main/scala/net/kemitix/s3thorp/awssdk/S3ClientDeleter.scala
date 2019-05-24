package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, Config, DeleteS3Action, RemoteKey}
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

private class S3ClientDeleter(s3Client: S3CatsIOClient)
  extends S3ClientLogging {

  def delete(bucket: Bucket,
             remoteKey: RemoteKey)
            (implicit c: Config): IO[DeleteS3Action] = {
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
