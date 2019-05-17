package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait S3Uploader
  extends S3Client
    with KeyGenerator
    with Logging {

  def enquedUpload(c: Config): ToUpload => IO[S3Action] = {
    val remoteKey = generateKey(c) _
    toUpload => {
      val file = toUpload.file
      val key = remoteKey(file)
      val shortFile = c.relativePath(file)
      log4(s"    Upload: $shortFile")(c)
      upload(file, c.bucket, key)
    }
  }
}
