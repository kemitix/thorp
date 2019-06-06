package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.s3thorp.aws.api.S3Action.DoNothingS3Action
import net.kemitix.s3thorp.aws.api.{S3Action, S3Client, UploadProgressListener}
import net.kemitix.s3thorp.domain.Config

object ActionSubmitter {

  def submitAction(s3Client: S3Client, action: Action)
                  (implicit c: Config,
                   info: Int => String => Unit,
                   warn: String => Unit): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          info(4)(s"    Upload: ${localFile.relative}")
          val progressListener = new UploadProgressListener(localFile)
          s3Client.upload(localFile, bucket, progressListener, c.multiPartThreshold, 1, c.maxRetries)
        case ToCopy(bucket, sourceKey, hash, targetKey) =>
          info(4)(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
          s3Client.copy(bucket, sourceKey, hash, targetKey)
        case ToDelete(bucket, remoteKey) =>
          info(4)(s"    Delete: ${remoteKey.key}")
          s3Client.delete(bucket, remoteKey)
        case DoNothing(bucket, remoteKey) => IO {
          DoNothingS3Action(remoteKey)}
      })
  }
}
