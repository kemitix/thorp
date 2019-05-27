package net.kemitix.s3thorp.awssdk
import cats.effect.IO
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.s3thorp._

class S3ClientMultiPartTransferManager(transferManager: TransferManager)
  extends S3ClientUploader
    with S3ClientMultiPartUploaderLogging {

  def accepts(localFile: LocalFile)
             (implicit c: Config): Boolean =
    localFile.file.length >= c.multiPartThreshold

  override
  def upload(localFile: LocalFile,
             bucket: Bucket,
             tryCount: Int)
            (implicit c: Config): IO[S3Action] = {
    IO {
      logMultiPartUploadStart(localFile, tryCount)
      val result = transferManager.upload(bucket.name, localFile.remoteKey.key, localFile.file)
        .waitForUploadResult()
      logMultiPartUploadFinished(localFile)
      UploadS3Action(RemoteKey(result.getKey), MD5Hash(result.getETag))
    }
  }
}
