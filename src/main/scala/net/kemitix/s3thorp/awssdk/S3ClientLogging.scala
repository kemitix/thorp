package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.{Bucket, Config, LocalFile, Logging}

trait S3ClientLogging
  extends Logging {

  def logUploadStart(localFile: LocalFile, bucket: Bucket)
                    (implicit c: Config)=
    log5(s"s3Client:upload:start: ${localFile.file}")

  def logUploadDone(localFile: LocalFile, bucket: Bucket)
                   (implicit c: Config) =
    log5(s"s3Client:upload:done : ${localFile.file}")

}
