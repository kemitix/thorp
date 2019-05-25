package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.{Config, LocalFile}
import software.amazon.awssdk.services.s3.model.{CreateMultipartUploadResponse, S3Exception, UploadPartRequest, UploadPartResponse}

trait S3ClientMultiPartUploaderLogging
  extends S3ClientLogging {

  def logMultiPartUploadStart(localFile: LocalFile,
                              tryCount: Int)
                             (implicit c: Config): Unit =
    log4(s"multi-part upload:try $tryCount: ${localFile.file.getPath}")

  def logMultiPartUploadInitiate(localFile: LocalFile)
                                (implicit c: Config): Unit =
    log5(s"multi-part upload:initiating: ${localFile.file.getPath}")

  def logMultiPartUploadPartDetails(localFile: LocalFile,
                                    nParts: Int,
                                    partSize: Long)
                                   (implicit c: Config): Unit =
    log5(s"multi-part upload:$nParts parts:$partSize each: ${localFile.file.getPath}")

  def logMultiPartUploadPart(localFile: LocalFile,
                             partRequest: UploadPartRequest)
                            (implicit c: Config): Unit =
    log5(s"multi-part upload:sending part ${partRequest.partNumber}: ${localFile.file.getPath}")

  def logMultiPartUploadPartError(localFile: LocalFile,
                                  partRequest: UploadPartRequest,
                                  error: Throwable)
                                 (implicit c: Config): Unit =
    warn(s"multi-part upload:part ${partRequest.partNumber}: ${localFile.file.getPath}")

  def logMultiPartUploadCompleted(createUploadResponse: CreateMultipartUploadResponse,
                                  uploadPartResponses: Stream[UploadPartResponse],
                                  localFile: LocalFile)
                                 (implicit c: Config): Unit =
    log4(s"multi-part upload:completed:${uploadPartResponses.size} parts: ${localFile.file.getPath}")

  def logMultiPartUploadCancelling(localFile: LocalFile)
                                  (implicit c: Config): Unit =
    warn(s"multi-part upload:cancelling: ${localFile.file.getPath}")
}
