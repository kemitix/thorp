package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import cats.implicits._
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._
import software.amazon.awssdk.services.s3.model.{Bucket => _, _}

import scala.util.control.NonFatal

private class S3ClientMultiPartUploader(s3Client: S3CatsIOClient)
  extends S3ClientMultiPartUploaderLogging
    with MD5HashGenerator
    with QuoteStripper {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean =
    localFile.file.length >= c.multiPartThreshold

  def createUpload(bucket: Bucket, localFile: LocalFile)
                  (implicit c: Config): IO[CreateMultipartUploadResponse] = {
    logMultiPartUploadInitiate(localFile)
    s3Client createMultipartUpload createUploadRequest(bucket, localFile)
  }

  def createUploadRequest(bucket: Bucket, localFile: LocalFile) = {
    CreateMultipartUploadRequest.builder
      .bucket(bucket.name)
      .key(localFile.remoteKey.key)
      .build
  }

  def parts(localFile: LocalFile,
            response: CreateMultipartUploadResponse)
           (implicit c: Config): IO[Stream[UploadPartRequest]] = {
    val fileSize = localFile.file.length
    val maxParts = 1024 // arbitrary, supports upto 10,000 (I, think)
    val threshold = c.multiPartThreshold
    val nParts = Math.min((fileSize / threshold) + 1, maxParts).toInt
    val partSize = fileSize / nParts
    val maxUpload = nParts * partSize

    IO {
      require(fileSize <= maxUpload,
        s"File (${localFile.file.getPath}) size ($fileSize) exceeds upload limit: $maxUpload")
      logMultiPartUploadPartDetails(localFile, nParts, partSize)
      for {
        partNumber <- (0 until nParts).toStream
        offSet = partNumber * partSize
        chunkSize = Math.min(fileSize - offSet, partSize)
        partHash = md5FilePart(localFile.file, offSet, chunkSize)
        uploadPartRequest = UploadPartRequest.builder
          .bucket(c.bucket.name)
          .uploadId(response.uploadId)
          .partNumber(partNumber)
          .contentLength(chunkSize)
          .contentMD5(partHash)
          .build
      } yield uploadPartRequest
    }
  }

  def uploadPart(localFile: LocalFile)
                (implicit c: Config): UploadPartRequest => IO[UploadPartResponse] =
    partRequest => {
      logMultiPartUploadPart(localFile, partRequest)
      s3Client.uploadPartFromFile(partRequest, localFile.file)
        .handleErrorWith(error => {
          logMultiPartUploadPartError(localFile, partRequest, error)
          IO.raiseError(CancellableMultiPartUpload(error, partRequest.uploadId))
        })
    }

  def uploadParts(localFile: LocalFile,
                  parts: Stream[UploadPartRequest])
                 (implicit c: Config): IO[Stream[UploadPartResponse]] =
    (parts map uploadPart(localFile)).sequence

  def completeUpload(createUploadResponse: CreateMultipartUploadResponse,
                     uploadPartResponses: Stream[UploadPartResponse],
                     localFile: LocalFile)
                    (implicit c: Config): IO[CompleteMultipartUploadResponse] = {
    logMultiPartUploadCompleted(createUploadResponse, uploadPartResponses, localFile)
    s3Client completeMultipartUpload createCompleteRequest(createUploadResponse)
  }

  def createCompleteRequest(createUploadResponse: CreateMultipartUploadResponse) = {
    CompleteMultipartUploadRequest.builder
      .uploadId(createUploadResponse.uploadId)
      .build
  }

  def cancel(uploadId: String, localFile: LocalFile)
            (implicit c: Config): IO[AbortMultipartUploadResponse] = {
    logMultiPartUploadCancelling(localFile)
    s3Client abortMultipartUpload AbortMultipartUploadRequest.builder
      .uploadId(uploadId)
      .build
  }

  def upload(localFile: LocalFile,
             bucket: Bucket,
             tryCount: Int)
            (implicit c: Config): IO[S3Action] = {
    logMultiPartUploadStart(localFile, tryCount)
    (for {
      createUploadResponse <- createUpload(bucket, localFile)
      parts <- parts(localFile, createUploadResponse)
      uploadPartResponses <- uploadParts(localFile, parts)
      completedUploadResponse <- completeUpload(createUploadResponse, uploadPartResponses, localFile)
    } yield completedUploadResponse)
      .map(_.eTag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
      .handleErrorWith {
        case CancellableMultiPartUpload(e, uploadId) =>
          if (tryCount >= 3) cancel(uploadId, localFile) *> IO.pure(ErroredS3Action(localFile.remoteKey, e))
          else IO(warn(e.getMessage)) *> upload(localFile, bucket, tryCount + 1)
        case NonFatal(e) =>
          if (tryCount >= 3) IO.pure(ErroredS3Action(localFile.remoteKey, e))
          else IO(warn(e.getMessage)) *> upload(localFile, bucket, tryCount + 1)
      }
  }
}
