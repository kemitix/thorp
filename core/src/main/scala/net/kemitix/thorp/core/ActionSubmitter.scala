package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.aws.api.S3Action.DoNothingS3Action
import net.kemitix.thorp.aws.api.{S3Action, S3Client, UploadProgressListener}
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.{Config, Logger}

object ActionSubmitter {

  def submitAction(s3Client: S3Client,
                   action: Action)
                  (implicit c: Config,
                   logger: Logger): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          for {
            _ <- logger.info(s"    Upload: ${localFile.relative}")
            progressListener = new UploadProgressListener(localFile)
            action <- s3Client.upload(localFile, bucket, progressListener, 1)
          } yield action
        case ToCopy(bucket, sourceKey, hash, targetKey) =>
          for {
            _ <- logger.info(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
            action <- s3Client.copy(bucket, sourceKey, hash, targetKey)
          } yield action
        case ToDelete(bucket, remoteKey) =>
          for {
            _ <- logger.info(s"    Delete: ${remoteKey.key}")
            action <- s3Client.delete(bucket, remoteKey)
          } yield action
        case DoNothing(bucket, remoteKey) =>
          IO.pure(DoNothingS3Action(remoteKey))
      })
  }
}
