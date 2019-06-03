package net.kemitix.s3thorp.awssdk

import com.amazonaws.services.s3.model.{AmazonS3Exception, InitiateMultipartUploadResult, UploadPartRequest, UploadPartResult}
import net.kemitix.s3thorp.domain.MD5Hash
import net.kemitix.s3thorp.{Config, LocalFile}

trait S3ClientMultiPartUploaderLogging
  extends S3ClientLogging {

  private val prefix = "multi-part upload"

  def logMultiPartUploadStart(localFile: LocalFile,
                              tryCount: Int)
                             (implicit c: Config): Unit =
    log1(s"$prefix:upload:try $tryCount: ${localFile.remoteKey.key}")

  def logMultiPartUploadFinished(localFile: LocalFile)
                                (implicit c: Config): Unit =
    log4(s"$prefix:upload:finished: ${localFile.remoteKey.key}")

  def logMultiPartUploadInitiate(localFile: LocalFile)
                                (implicit c: Config): Unit =
    log5(s"$prefix:initiating: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartsDetails(localFile: LocalFile,
                                     nParts: Int,
                                     partSize: Long)
                                    (implicit c: Config): Unit =
    log5(s"$prefix:parts $nParts:each $partSize: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartDetails(localFile: LocalFile,
                                    partNumber: Int,
                                    partHash: MD5Hash)
                                   (implicit c: Config): Unit =
    log5(s"$prefix:part $partNumber:hash ${partHash.hash}: ${localFile.remoteKey.key}")

  def logMultiPartUploadPart(localFile: LocalFile,
                             partRequest: UploadPartRequest)
                            (implicit c: Config): Unit =
    log5(s"$prefix:sending:part ${partRequest.getPartNumber}: ${partRequest.getMd5Digest}: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartDone(localFile: LocalFile,
                                 partRequest: UploadPartRequest,
                                 result: UploadPartResult)
                                (implicit c: Config): Unit =
    log5(s"$prefix:sent:part ${partRequest.getPartNumber}: ${result.getPartETag}: ${localFile.remoteKey.key}")

  def logMultiPartUploadPartError(localFile: LocalFile,
                                  partRequest: UploadPartRequest,
                                  error: AmazonS3Exception)
                                 (implicit c: Config): Unit = {
    val returnedMD5Hash = error.getAdditionalDetails.get("Content-MD5")
    warn(s"$prefix:error:part ${partRequest.getPartNumber}:ret-hash $returnedMD5Hash: ${localFile.remoteKey.key}")
  }

  def logMultiPartUploadCompleted(createUploadResponse: InitiateMultipartUploadResult,
                                  uploadPartResponses: Stream[UploadPartResult],
                                  localFile: LocalFile)
                                 (implicit c: Config): Unit =
    log1(s"$prefix:completed:parts ${uploadPartResponses.size}: ${localFile.remoteKey.key}")

  def logMultiPartUploadCancelling(localFile: LocalFile)
                                  (implicit c: Config): Unit =
    warn(s"$prefix:cancelling: ${localFile.remoteKey.key}")

  def logErrorRetrying(e: Throwable, localFile: LocalFile, tryCount: Int)
                      (implicit c: Config): Unit =
    warn(s"$prefix:retry:error ${e.getMessage}: ${localFile.remoteKey.key}")

  def logErrorCancelling(e: Throwable, localFile: LocalFile)
                        (implicit c: Config) : Unit =
    error(s"$prefix:cancelling:error ${e.getMessage}: ${localFile.remoteKey.key}")

  def logErrorUnknown(e: Throwable, localFile: LocalFile)
                     (implicit c: Config): Unit =
    error(s"$prefix:unknown:error $e: ${localFile.remoteKey.key}")

}
