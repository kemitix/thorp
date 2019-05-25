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

  def parts(localFile: LocalFile,
            response: CreateMultipartUploadResponse)
           (implicit c: Config): IO[Stream[UploadPartRequest]] =
    {
      val fileSize = localFile.file.length
      val maxParts = 1024 // arbitrary, supports upto 10,000 (I, think)
      val threshold = c.multiPartThreshold
      val nParts = Math.min((fileSize / threshold) + 1, maxParts).toInt
      val partSize = fileSize / nParts
      val maxUpload = nParts * partSize

      IO {
        require(fileSize <= maxUpload,
          s"File (${localFile.file.getPath}) size ($fileSize) exceeds upload limit: $maxUpload")
        for {
          partNumber <- (0 until nParts).toStream
          offSet = partNumber * partSize
          chunkSize = Math.min(fileSize - offSet, partSize)
          partHash = md5FilePart(localFile.file, offSet, chunkSize)
          uploadPartRequest = UploadPartRequest.builder
            .uploadId(response.uploadId)
            .partNumber(partNumber)
            .contentLength(chunkSize)
            .contentMD5(partHash)
            .build
        } yield uploadPartRequest
      }
    }

  def uploadPart(localFile: LocalFile): UploadPartRequest => IO[UploadPartResponse] =
    s3Client.uploadPartFromFile(_, localFile.file)

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
