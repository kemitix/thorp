package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait ActionSubmitter
  extends S3Client
    with Logging {

  def submitAction(action: Action)
                  (implicit c: Config): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(file) =>
          log4(s"    Upload: ${file.relative}")
          upload(file, c.bucket)
        case ToCopy(sourceKey, hash, targetKey) =>
          log4(s"      Copy: $sourceKey => $targetKey")
          copy(c.bucket, sourceKey, hash, targetKey)
        case ToDelete(remoteKey) =>
          log4(s"    Delete: $remoteKey")
          delete(c.bucket, remoteKey)
        case DoNothing(remoteKey) => IO {
          DoNothingS3Action(remoteKey)}
      })
  }
}
