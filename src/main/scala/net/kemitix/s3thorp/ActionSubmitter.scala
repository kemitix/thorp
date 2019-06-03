package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.{S3Client, UploadProgressListener}
import net.kemitix.s3thorp.domain.Config

trait ActionSubmitter
  extends S3Client
    with Logging {

  def submitAction(action: Action)
                  (implicit c: Config): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(localFile) =>
          log4(s"    Upload: ${localFile.relative}")
          val progressListener = new UploadProgressListener(localFile)
          upload(localFile, c.bucket, progressListener, 1)
        case ToCopy(sourceKey, hash, targetKey) =>
          log4(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
          copy(c.bucket, sourceKey, hash, targetKey)
        case ToDelete(remoteKey) =>
          log4(s"    Delete: ${remoteKey.key}")
          delete(c.bucket, remoteKey)
        case DoNothing(remoteKey) => IO {
          DoNothingS3Action(remoteKey)}
      })
  }
}
