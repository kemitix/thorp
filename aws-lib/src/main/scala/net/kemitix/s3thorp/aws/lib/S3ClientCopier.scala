package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.aws.api.S3Action.CopyS3Action
import net.kemitix.s3thorp.domain.{Bucket, MD5Hash, RemoteKey}
import software.amazon.awssdk.services.s3.model.CopyObjectRequest

class S3ClientCopier(s3Client: S3CatsIOClient)
  extends S3ClientLogging {

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey)
          (implicit info: Int => String => Unit): IO[CopyS3Action] = {
    val request = CopyObjectRequest.builder
      .bucket(bucket.name)
      .copySource(s"${bucket.name}/${sourceKey.key}")
      .copySourceIfMatch(hash.hash)
      .key(targetKey.key).build
    s3Client.copyObject(request)
      .bracket(
        logCopyStart(bucket, sourceKey, targetKey))(
        logCopyFinish(bucket, sourceKey,targetKey))
      .map(_ => CopyS3Action(targetKey))
  }

}