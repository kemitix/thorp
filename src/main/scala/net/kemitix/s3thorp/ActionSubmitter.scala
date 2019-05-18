package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait ActionSubmitter
  extends S3Client
    with KeyGenerator
    with Logging {

  def submitAction(action: Action)
                  (implicit c: Config): IO[S3Action] = {
    action match {
      case ToUpload(file) =>
        log4(s"    Upload: ${c.relativePath(file)}")
        upload(file, c.bucket, generateKey(file))
    }
  }
}
