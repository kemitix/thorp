package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import net.kemitix.s3thorp._
import net.kemitix.s3thorp.domain.{Bucket, Config, LocalFile, MD5Hash}

class S3ClientPutObjectUploader(s3Client: => AmazonS3)
  extends S3ClientUploader
    with S3ClientLogging
    with QuoteStripper {

  override def accepts(localFile: LocalFile)(implicit c: Config): Boolean = true

  override
  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             tryCount: Int)
            (implicit c: Config): IO[UploadS3Action] = {
    val request: PutObjectRequest =
      new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
        .withGeneralProgressListener(progressListener(uploadProgressListener))
    IO(s3Client.putObject(request))
      .bracket(
        logUploadStart(localFile, bucket))(
        logUploadFinish(localFile, bucket))
      .map(_.getETag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
  }
}
