package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import cats.implicits._
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._
import software.amazon.awssdk.services.s3.model.{Bucket => _, _}

private class S3ClientMultiPartUploader(s3Client: S3CatsIOClient)
  extends S3ClientLogging
    with MD5HashGenerator
    with QuoteStripper {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean =
    localFile.file.length >= c.multiPartThreshold

  def createUpload(bucket: Bucket, localFile: LocalFile): IO[CreateMultipartUploadResponse] =
    s3Client createMultipartUpload
      CreateMultipartUploadRequest.builder
        .bucket(bucket.name)
        .key(localFile.remoteKey.key)
        .build

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
