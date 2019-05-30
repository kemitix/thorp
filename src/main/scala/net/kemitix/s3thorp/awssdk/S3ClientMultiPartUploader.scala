package net.kemitix.s3thorp.awssdk

import scala.collection.JavaConverters._
import cats.effect.IO
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{AbortMultipartUploadRequest, AmazonS3Exception, CompleteMultipartUploadRequest, CompleteMultipartUploadResult, InitiateMultipartUploadRequest, InitiateMultipartUploadResult, PartETag, UploadPartRequest, UploadPartResult}
import net.kemitix.s3thorp._

import scala.util.control.NonFatal

private class S3ClientMultiPartUploader(s3Client: AmazonS3)
  extends S3ClientUploader
    with S3ClientMultiPartUploaderLogging
    with MD5HashGenerator
    with QuoteStripper {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean =
    localFile.file.length >= c.multiPartThreshold

  def createUpload(bucket: Bucket, localFile: LocalFile)
                  (implicit c: Config): IO[InitiateMultipartUploadResult] = {
    logMultiPartUploadInitiate(localFile)
    IO(s3Client initiateMultipartUpload createUploadRequest(bucket, localFile))
  }

  def createUploadRequest(bucket: Bucket, localFile: LocalFile) =
    new InitiateMultipartUploadRequest(
      bucket.name,
      localFile.remoteKey.key)

  def parts(localFile: LocalFile,
            response: InitiateMultipartUploadResult)
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
      logMultiPartUploadPartsDetails(localFile, nParts, partSize)
      for {
        partNumber <- (1 to nParts).toStream
        offSet = (partNumber - 1) * partSize
        chunkSize = Math.min(fileSize - offSet, partSize)
        partHash = md5FilePart(localFile.file, offSet, chunkSize)
        _ = logMultiPartUploadPartDetails(localFile, partNumber, partHash)
        uploadPartRequest = createUploadPartRequest(localFile, response, partNumber, chunkSize, partHash)
      } yield uploadPartRequest
    }
  }

  private def createUploadPartRequest(localFile: LocalFile,
                                      response: InitiateMultipartUploadResult,
                                      partNumber: Int,
                                      chunkSize: Long,
                                      partHash: MD5Hash)
                                     (implicit c: Config) = {
    new UploadPartRequest()
      .withBucketName(c.bucket.name)
      .withKey(localFile.remoteKey.key)
      .withUploadId(response.getUploadId)
      .withPartNumber(partNumber)
      .withPartSize(chunkSize)
      .withMD5Digest(partHash.hash)
      .withFile(localFile.file)
      .withFileOffset((partNumber - 1) * chunkSize)
  }

  def uploadPart(localFile: LocalFile)
                (implicit c: Config): UploadPartRequest => IO[UploadPartResult] =
    partRequest => {
      logMultiPartUploadPart(localFile, partRequest)
      IO(s3Client.uploadPart(partRequest))
        .handleErrorWith{
          case error: AmazonS3Exception => {
          logMultiPartUploadPartError(localFile, partRequest, error)
          IO.raiseError(CancellableMultiPartUpload(error, partRequest.getUploadId))
        }}
    }

  def uploadParts(localFile: LocalFile,
                  parts: Stream[UploadPartRequest])
                 (implicit c: Config): IO[Stream[UploadPartResult]] =
    (parts map uploadPart(localFile)).sequence

  def completeUpload(createUploadResponse: InitiateMultipartUploadResult,
                     uploadPartResponses: Stream[UploadPartResult],
                     localFile: LocalFile)
                    (implicit c: Config): IO[CompleteMultipartUploadResult] = {
    logMultiPartUploadCompleted(createUploadResponse, uploadPartResponses, localFile)
    IO(s3Client completeMultipartUpload createCompleteRequest(createUploadResponse, uploadPartResponses.toList))
  }

  def createCompleteRequest(createUploadResponse: InitiateMultipartUploadResult,
                            uploadPartResult: List[UploadPartResult]) = {
    new CompleteMultipartUploadRequest()
      .withBucketName(createUploadResponse.getBucketName)
      .withKey(createUploadResponse.getKey)
      .withUploadId(createUploadResponse.getUploadId)
      .withPartETags(uploadPartResult.asJava)
  }

  def cancel(uploadId: String, localFile: LocalFile)
            (implicit c: Config): IO[Unit] = {
    logMultiPartUploadCancelling(localFile)
    IO(s3Client abortMultipartUpload createAbortRequest(uploadId, localFile))
  }

  def createAbortRequest(uploadId: String,
                         localFile: LocalFile)
                        (implicit c: Config): AbortMultipartUploadRequest =
    new AbortMultipartUploadRequest(c.bucket.name, localFile.remoteKey.key, uploadId)

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      tryCount: Int)
                     (implicit c: Config): IO[S3Action] = {
    logMultiPartUploadStart(localFile, tryCount)

    (for {
      createUploadResponse <- createUpload(bucket, localFile)
      parts <- parts(localFile, createUploadResponse)
      uploadPartResponses <- uploadParts(localFile, parts)
      completedUploadResponse <- completeUpload(createUploadResponse, uploadPartResponses, localFile)
    } yield completedUploadResponse)
      .map(_.getETag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
      .handleErrorWith {
        case CancellableMultiPartUpload(e, uploadId) =>
          if (tryCount >= c.maxRetries) IO(logErrorCancelling(e, localFile)) *> cancel(uploadId, localFile) *> IO.pure(ErroredS3Action(localFile.remoteKey, e))
          else IO(logErrorRetrying(e, localFile, tryCount)) *> upload(localFile, bucket, progressListener, tryCount + 1)
        case NonFatal(e) =>
          if (tryCount >= c.maxRetries) IO(logErrorUnknown(e, localFile)) *> IO.pure(ErroredS3Action(localFile.remoteKey, e))
          else IO(logErrorRetrying(e, localFile, tryCount)) *> upload(localFile, bucket, progressListener, tryCount + 1)
      }
  }
}
