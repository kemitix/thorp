package net.kemitix.thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import net.kemitix.thorp.aws.api.S3Action.DeleteS3Action
import net.kemitix.thorp.aws.lib.S3ClientLogging.{logDeleteFinish, logDeleteStart}
import net.kemitix.thorp.domain.{Bucket, Logger, RemoteKey}

class S3ClientDeleter(amazonS3: AmazonS3) {

  def delete(bucket: Bucket,
             remoteKey: RemoteKey)
            (implicit logger: Logger): IO[DeleteS3Action] =
    for {
      _ <- logDeleteStart(bucket, remoteKey)
      _ <- deleteObject(bucket, remoteKey)
      _ <- logDeleteFinish(bucket, remoteKey)
    } yield DeleteS3Action(remoteKey)

  private def deleteObject(bucket: Bucket, remoteKey: RemoteKey) =
    IO(amazonS3.deleteObject(new DeleteObjectRequest(bucket.name, remoteKey.key)))

}
