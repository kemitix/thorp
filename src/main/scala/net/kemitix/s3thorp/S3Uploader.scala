package net.kemitix.s3thorp

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait S3Uploader
  extends S3Client
    with KeyGenerator
    with Logging {

  def performUpload(c: Config): ToUpload => IO[(File, Either[Throwable, MD5Hash])] = {
    val remoteKey = generateKey(c) _
    toUpload => {
      val file = toUpload.file
      val key = remoteKey(file)
      val shortFile = c.relativePath(file)
      log4(s"    Upload: $shortFile")(c)
      upload(file, c.bucket, key).map(result => (file, result))
    }
  }
}
