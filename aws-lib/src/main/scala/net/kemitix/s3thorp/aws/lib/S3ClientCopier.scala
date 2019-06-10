package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import net.kemitix.s3thorp.aws.api.S3Action.CopyS3Action
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logCopyFinish, logCopyStart}
import net.kemitix.s3thorp.domain.{Bucket, MD5Hash, RemoteKey}

class S3ClientCopier(amazonS3: AmazonS3) {

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey)
          (implicit info: Int => String => IO[Unit]): IO[CopyS3Action] = {
    IO {
      new CopyObjectRequest(
        bucket.name, sourceKey.key,
        bucket.name, targetKey.key)
        .withMatchingETagConstraint(hash.hash)
    }.bracket {
      request =>
        for {
          _ <- logCopyStart(bucket, sourceKey, targetKey)
          result <- IO(amazonS3.copyObject(request))
        } yield result
    }(_ => logCopyFinish(bucket, sourceKey,targetKey))
      .map(_ => CopyS3Action(targetKey))
  }

}
