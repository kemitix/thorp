package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.s3thorp._
import net.kemitix.s3thorp.domain.Bucket

class S3ClientMultiPartTransferManager(transferManager: => TransferManager)
  extends S3ClientUploader
    with S3ClientMultiPartUploaderLogging {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean =
    localFile.file.length >= c.multiPartThreshold

  override
  def upload(localFile: LocalFile,
             bucket: Bucket,
             progressListener: UploadProgressListener,
             tryCount: Int)
            (implicit c: Config): IO[S3Action] = {
    val putObjectRequest: PutObjectRequest =
      new PutObjectRequest(bucket.name, localFile.remoteKey.key, localFile.file)
        .withGeneralProgressListener(progressListener.listener)
    IO {
      logMultiPartUploadStart(localFile, tryCount)
      val result = transferManager.upload(putObjectRequest)
        .waitForUploadResult()
      logMultiPartUploadFinished(localFile)
      UploadS3Action(RemoteKey(result.getKey), MD5Hash(result.getETag))
    }
  }
}
