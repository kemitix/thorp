package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import net.kemitix.s3thorp.S3Action.UploadS3Action
import net.kemitix.s3thorp.domain.{Bucket, Config, LocalFile, MD5Hash}

class S3ClientPutObjectUploader(amazonS3: => AmazonS3)
  extends S3ClientUploader
    with S3ClientLogging
    with QuoteStripper {

  override def accepts(localFile: LocalFile)(implicit multiPartThreshold: Long): Boolean = true

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      uploadProgressListener: UploadProgressListener,
                      multiPartThreshold: Long,
                      tryCount: Int,
                      maxRetries: Int)
                     (implicit info: Int => String => Unit,
                      warn: String => Unit): IO[UploadS3Action] = {
    val request = putObjectRequest(localFile, bucket, uploadProgressListener)
    IO(amazonS3.putObject(request))
      .bracket(
        logUploadStart(localFile, bucket))(
        logUploadFinish(localFile, bucket))
      .map(_.getETag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
  }

  private def putObjectRequest(localFile: LocalFile,
                               bucket: Bucket,
                               uploadProgressListener: UploadProgressListener
                              ): PutObjectRequest = {
    new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
      .withGeneralProgressListener(progressListener(uploadProgressListener))
  }
}
