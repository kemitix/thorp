package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import net.kemitix.s3thorp.{Bucket, Config, LocalFile, S3Action}

trait S3ClientUploader {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean

  def upload(localFile: LocalFile,
             bucket: Bucket,
             progressListener: UploadProgressListener,
             tryCount: Int)
            (implicit c: Config): IO[S3Action]

}
