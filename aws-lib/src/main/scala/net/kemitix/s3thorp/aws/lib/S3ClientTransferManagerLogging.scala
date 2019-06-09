package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.model.{AmazonS3Exception, InitiateMultipartUploadResult, UploadPartRequest, UploadPartResult}
import net.kemitix.s3thorp.domain.{LocalFile, MD5Hash}

object S3ClientTransferManagerLogging {

  def logMultiPartUploadStart(localFile: LocalFile,
                              tryCount: Int)
                             (implicit info: Int => String => IO[Unit]): IO[Unit] =
    {
      val tryMessage = if (tryCount == 1) "" else s"try $tryCount"
      val size = localFile.file.length
      info(1)(s"upload:$tryMessage:$size:${localFile.remoteKey.key}")
    }

  def logMultiPartUploadFinished(localFile: LocalFile)
                                (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(4)(s"upload:finished: ${localFile.remoteKey.key}")

  def logMultiPartUploadInitiate(localFile: LocalFile)
                                (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(5)(s"initiating: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartsDetails(localFile: LocalFile,
                                     nParts: Int,
                                     partSize: Long)
                                    (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(5)(s"parts $nParts:each $partSize: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartDetails(localFile: LocalFile,
                                    partNumber: Int,
                                    partHash: MD5Hash)
                                   (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(5)(s"part $partNumber:hash ${partHash.hash}: ${localFile.remoteKey.key}")

  def logMultiPartUploadPart(localFile: LocalFile,
                             partRequest: UploadPartRequest)
                            (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(5)(s"sending:part ${partRequest.getPartNumber}: ${partRequest.getMd5Digest}: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartDone(localFile: LocalFile,
                                 partRequest: UploadPartRequest,
                                 result: UploadPartResult)
                                (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(5)(s"sent:part ${partRequest.getPartNumber}: ${result.getPartETag}: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartError(localFile: LocalFile,
                                  partRequest: UploadPartRequest,
                                  error: AmazonS3Exception)
                                 (implicit warn: String => IO[Unit]): IO[Unit] = {
    val returnedMD5Hash = error.getAdditionalDetails.get("Content-MD5")
    warn(s"error:part ${partRequest.getPartNumber}:ret-hash $returnedMD5Hash: ${localFile.remoteKey.key}")
  }

  def logMultiPartUploadCompleted(createUploadResponse: InitiateMultipartUploadResult,
                                  uploadPartResponses: Stream[UploadPartResult],
                                  localFile: LocalFile)
                                 (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(1)(s"completed:parts ${uploadPartResponses.size}: ${localFile.remoteKey.key}")

  def logMultiPartUploadCancelling(localFile: LocalFile)
                                  (implicit warn: String => IO[Unit]): IO[Unit] =
    warn(s"cancelling: ${localFile.remoteKey.key}")

  def logErrorRetrying(e: Throwable, localFile: LocalFile, tryCount: Int)
                      (implicit warn: String => IO[Unit]): IO[Unit] =
    warn(s"retry:error ${e.getMessage}: ${localFile.remoteKey.key}")

  def logErrorCancelling(e: Throwable, localFile: LocalFile)
                        (implicit error: String => IO[Unit]) : IO[Unit] =
    error(s"cancelling:error ${e.getMessage}: ${localFile.remoteKey.key}")

  def logErrorUnknown(e: Throwable, localFile: LocalFile)
                     (implicit error: String => IO[Unit]): IO[Unit] =
    error(s"unknown:error $e: ${localFile.remoteKey.key}")

}
