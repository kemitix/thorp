package net.kemitix.s3thorp

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.Sync.MD5Hash
import net.kemitix.s3thorp.awssdk.S3Client

trait S3Uploader
  extends S3Client
    with KeyGenerator
    with Logging {

  def performUpload(c: Config): File => (File, IO[Either[Throwable, MD5Hash]]) = {
    val remoteKey = generateKey(c) _
    file => {
      val key = remoteKey(file)
      val shortFile = c.relativePath(file)
      log4(s"    Upload: $shortFile")(c)
      (file, upload(file, c.bucket, key))
    }
  }
}
