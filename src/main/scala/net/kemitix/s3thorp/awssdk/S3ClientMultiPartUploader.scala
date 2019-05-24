package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, Config, LocalFile, MD5Hash, UploadS3Action}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest

private class S3ClientMultiPartUploader(s3Client: S3CatsIOClient)
  extends S3ClientLogging
    with QuoteStripper {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean =
    localFile.file.length >= c.multiPartThreshold

  def upload(localFile: LocalFile,
             bucket: Bucket)
            (implicit c: Config): IO[UploadS3Action] = {
    val request = PutObjectRequest.builder
      .bucket(bucket.name)
      .key(localFile.remoteKey.key).build
    val body = AsyncRequestBody.fromFile(localFile.file)
    s3Client.putObject(request, body)
      .bracket(
        logUploadStart(localFile, bucket))(
        logUploadFinish(localFile, bucket))
      .map(_.eTag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
  }
}
