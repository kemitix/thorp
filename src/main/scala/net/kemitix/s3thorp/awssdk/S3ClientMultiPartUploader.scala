package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import net.kemitix.s3thorp.MD5HashGenerator.md5FilePart
import net.kemitix.s3thorp.aws.api.S3Action.{ErroredS3Action, UploadS3Action}
import net.kemitix.s3thorp.aws.api.{S3Action, UploadProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, MD5Hash}

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

private class S3ClientMultiPartUploader(s3Client: AmazonS3)
  extends S3ClientUploader
    with S3ClientMultiPartUploaderLogging
    with QuoteStripper {

  def accepts(localFile: LocalFile)
             (implicit multiPartThreshold: Long): Boolean =
    localFile.file.length >= multiPartThreshold

  def createUpload(bucket: Bucket, localFile: LocalFile)
                  (implicit info: Int => String => Unit): IO[InitiateMultipartUploadResult] = {
    logMultiPartUploadInitiate(localFile)
    IO(s3Client initiateMultipartUpload createUploadRequest(bucket, localFile))
  }

  def createUploadRequest(bucket: Bucket, localFile: LocalFile) =
    new InitiateMultipartUploadRequest(
      bucket.name,
      localFile.remoteKey.key)

  def parts(bucket: Bucket,
            localFile: LocalFile,
            response: InitiateMultipartUploadResult,
            threshold: Long)
           (implicit info: Int => String => Unit): IO[Stream[UploadPartRequest]] = {
    val fileSize = localFile.file.length
    val maxParts = 1024 // arbitrary, supports upto 10,000 (I, think)
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
        uploadPartRequest = createUploadPartRequest(bucket, localFile, response, partNumber, chunkSize, partHash)
      } yield uploadPartRequest
    }
  }

  private def createUploadPartRequest(bucket: Bucket,
                                      localFile: LocalFile,
                                      response: InitiateMultipartUploadResult,
                                      partNumber: Int,
                                      chunkSize: Long,
                                      partHash: MD5Hash) = {
    new UploadPartRequest()
      .withBucketName(bucket.name)
      .withKey(localFile.remoteKey.key)
      .withUploadId(response.getUploadId)
      .withPartNumber(partNumber)
      .withPartSize(chunkSize)
      .withMD5Digest(partHash.hash)
      .withFile(localFile.file)
      .withFileOffset((partNumber - 1) * chunkSize)
  }

  def uploadPart(localFile: LocalFile)
                (implicit info: Int => String => Unit,
                 warn: String => Unit): UploadPartRequest => IO[UploadPartResult] =
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
                 (implicit info: Int => String => Unit,
                  warn: String => Unit): IO[Stream[UploadPartResult]] =
    (parts map uploadPart(localFile)).sequence

  def completeUpload(createUploadResponse: InitiateMultipartUploadResult,
                     uploadPartResponses: Stream[UploadPartResult],
                     localFile: LocalFile)
                    (implicit info: Int => String => Unit): IO[CompleteMultipartUploadResult] = {
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

  def cancel(uploadId: String,
             bucket: Bucket,
             localFile: LocalFile)
            (implicit info: Int => String => Unit,
             warn: String => Unit): IO[Unit] = {
    logMultiPartUploadCancelling(localFile)
    IO(s3Client abortMultipartUpload createAbortRequest(uploadId, bucket, localFile))
  }

  def createAbortRequest(uploadId: String,
                         bucket: Bucket,
                         localFile: LocalFile): AbortMultipartUploadRequest =
    new AbortMultipartUploadRequest(bucket.name, localFile.remoteKey.key, uploadId)

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      multiPartThreshold: Long,
                      tryCount: Int,
                      maxRetries: Int)
                     (implicit info: Int => String => Unit,
                      warn: String => Unit): IO[S3Action] = {
    logMultiPartUploadStart(localFile, tryCount)

    (for {
      createUploadResponse <- createUpload(bucket, localFile)
      parts <- parts(bucket, localFile, createUploadResponse, multiPartThreshold)
      uploadPartResponses <- uploadParts(localFile, parts)
      completedUploadResponse <- completeUpload(createUploadResponse, uploadPartResponses, localFile)
    } yield completedUploadResponse)
      .map(_.getETag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
      .handleErrorWith {
        case CancellableMultiPartUpload(e, uploadId) =>
          if (tryCount >= maxRetries) IO(logErrorCancelling(e, localFile)) *> cancel(uploadId, bucket, localFile) *> IO.pure(ErroredS3Action(localFile.remoteKey, e))
          else IO(logErrorRetrying(e, localFile, tryCount)) *> upload(localFile, bucket, progressListener, multiPartThreshold, tryCount + 1, maxRetries)
        case NonFatal(e) =>
          if (tryCount >= maxRetries) IO(logErrorUnknown(e, localFile)) *> IO.pure(ErroredS3Action(localFile.remoteKey, e))
          else IO(logErrorRetrying(e, localFile, tryCount)) *> upload(localFile, bucket, progressListener, multiPartThreshold, tryCount + 1, maxRetries)
      }
  }
}
