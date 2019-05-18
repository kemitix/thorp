package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait S3Uploader
  extends S3Client
    with KeyGenerator
    with Logging {

  def enquedUpload(toUpload: ToUpload)(implicit c: Config): IO[S3Action] = {
    val file = toUpload.file
    val key = generateKey(file)
    val shortFile = c.relativePath(file)
    log4(s"    Upload: $shortFile")(c)
    upload(file, c.bucket, key)
  }
}
