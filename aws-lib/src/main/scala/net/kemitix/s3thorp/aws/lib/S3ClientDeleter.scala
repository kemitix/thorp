package net.kemitix.s3thorp.aws.lib

import cats.Monad
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.s3thorp.aws.api.S3Action.DeleteS3Action
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logDeleteFinish, logDeleteStart}
import net.kemitix.s3thorp.domain.{Bucket, RemoteKey}

class S3ClientDeleter[M[_]: Monad](amazonS3: AmazonS3) {

  def delete(bucket: Bucket,
             remoteKey: RemoteKey)
            (implicit info: Int => String => M[Unit]): M[DeleteS3Action] =
    for {
      _ <- logDeleteStart[M](bucket, remoteKey)
      _ <- deleteObject(bucket, remoteKey)
      _ <- logDeleteFinish[M](bucket, remoteKey)
    } yield DeleteS3Action(remoteKey)

  private def deleteObject(bucket: Bucket, remoteKey: RemoteKey) = Monad[M].pure {
    amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key))
  }
}
