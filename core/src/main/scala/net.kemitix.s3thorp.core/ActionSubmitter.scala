package net.kemitix.s3thorp.core

import cats.Monad
import cats.implicits._
import net.kemitix.s3thorp.aws.api.S3Action.DoNothingS3Action
import net.kemitix.s3thorp.aws.api.{S3Action, S3Client, UploadProgressListener}
import net.kemitix.s3thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.{Config, Logger}

object ActionSubmitter {

  def submitAction[M[_]: Monad](s3Client: S3Client[M], action: Action)
                               (implicit c: Config,
                                logger: Logger[M]): Stream[M[S3Action]] = {
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          for {
            _ <- logger.info(s"    Upload: ${localFile.relative}")
            progressListener = new UploadProgressListener(localFile)
            action <- s3Client.upload(localFile, bucket, progressListener, c.multiPartThreshold, 1, c.maxRetries)
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
          Monad[M].pure(DoNothingS3Action(remoteKey))
      })
  }
}
