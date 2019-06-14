package net.kemitix.s3thorp.aws.lib

import cats.Monad
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import net.kemitix.s3thorp.aws.api.S3Action.CopyS3Action
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logCopyFinish, logCopyStart}
import net.kemitix.s3thorp.domain.{Bucket, Logger, MD5Hash, RemoteKey}

class S3ClientCopier[M[_]: Monad](amazonS3: AmazonS3) {

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey)
          (implicit logger: Logger[M]): M[CopyS3Action] =
  for {
    _ <- logCopyStart[M](bucket, sourceKey, targetKey)
    _ <- copyObject(bucket, sourceKey, hash, targetKey)
    _ <- logCopyFinish[M](bucket, sourceKey,targetKey)
  } yield CopyS3Action(targetKey)

  private def copyObject(bucket: Bucket,
                         sourceKey: RemoteKey,
                         hash: MD5Hash,
                         targetKey: RemoteKey) = {
    val request =
      new CopyObjectRequest(bucket.name, sourceKey.key, bucket.name, targetKey.key)
        .withMatchingETagConstraint(hash.hash)
    Monad[M].pure(amazonS3.copyObject(request))
  }

}
