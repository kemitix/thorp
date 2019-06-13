package net.kemitix.s3thorp.core

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.S3Action.DoNothingS3Action
import net.kemitix.s3thorp.aws.api.{S3Action, S3Client, UploadProgressListener}
import net.kemitix.s3thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.s3thorp.domain.Config

object ActionSubmitter {

  def submitAction(s3Client: S3Client, action: Action)
                  (implicit c: Config,
                   info: Int => String => IO[Unit],
                   warn: String => IO[Unit]): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          for {
            _ <- info(4) (s"    Upload: ${localFile.relative}")
            progressListener = new UploadProgressListener[IO](localFile)
            action <- s3Client.upload(localFile, bucket, progressListener, c.multiPartThreshold, 1, c.maxRetries)
          } yield action
        case ToCopy(bucket, sourceKey, hash, targetKey) =>
          for {
            _ <- info(4)(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
            action <- s3Client.copy(bucket, sourceKey, hash, targetKey)
          } yield action
        case ToDelete(bucket, remoteKey) =>
          for {
            _ <- info(4)(s"    Delete: ${remoteKey.key}")
            action <- s3Client.delete(bucket, remoteKey)
          } yield action
        case DoNothing(bucket, remoteKey) =>
          IO.pure(DoNothingS3Action(remoteKey))
      })
  }
}
