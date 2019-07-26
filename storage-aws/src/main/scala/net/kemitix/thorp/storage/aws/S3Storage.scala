package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.StorageQueueEvent.ShutdownQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.Storage
import zio.{TaskR, UIO}

class S3Storage(
    amazonS3Client: => AmazonS3.Client,
    amazonTransferManager: => AmazonTransferManager
) extends Storage.Service {

  override def listObjects(
      bucket: Bucket,
      prefix: RemoteKey
  ): TaskR[Console, S3ObjectsData] =
    Lister.listObjects(amazonS3Client)(bucket, prefix)

  override def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): UIO[StorageQueueEvent] =
    Copier.copy(amazonS3Client)(bucket, sourceKey, hash, targetKey)

  override def upload(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): UIO[StorageQueueEvent] =
    Uploader.upload(amazonTransferManager)(localFile,
                                           bucket,
                                           batchMode,
                                           uploadEventListener,
                                           1)

  override def delete(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): UIO[StorageQueueEvent] =
    Deleter.delete(amazonS3Client)(bucket, remoteKey)

  override def shutdown: UIO[StorageQueueEvent] = {
    amazonTransferManager.shutdownNow(true)
    amazonS3Client.shutdown().map(_ => ShutdownQueueEvent())
  }

}
