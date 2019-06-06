package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.s3thorp.S3Action.DoNothingS3Action
import net.kemitix.s3thorp.awssdk.{S3Client, UploadProgressListener}
import net.kemitix.s3thorp.domain.Config

object ActionSubmitter
    extends Logging {

  def submitAction(s3Client: S3Client, action: Action)
                  (implicit c: Config): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(localFile) =>
          log4(s"    Upload: ${localFile.relative}")
          val progressListener = new UploadProgressListener(localFile)
          s3Client.upload(localFile, c.bucket, progressListener, 1)
        case ToCopy(sourceKey, hash, targetKey) =>
          log4(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
          s3Client.copy(c.bucket, sourceKey, hash, targetKey)
        case ToDelete(remoteKey) =>
          log4(s"    Delete: ${remoteKey.key}")
          s3Client.delete(c.bucket, remoteKey)
        case DoNothing(remoteKey) => IO {
          DoNothingS3Action(remoteKey)}
      })
  }
}
