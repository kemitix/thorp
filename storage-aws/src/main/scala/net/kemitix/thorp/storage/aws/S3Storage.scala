package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.StorageQueueEvent.ShutdownQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.Storage
import zio.{Task, TaskR, UIO}

class S3Storage(
    amazonS3Client: => AmazonS3.Client,
    amazonTransferManager: => AmazonTransferManager
) extends Storage.Service {

  lazy val objectLister = new Lister(amazonS3Client)
  lazy val copier       = new Copier(amazonS3Client)
  lazy val uploader     = new Uploader(amazonTransferManager)
  lazy val deleter      = new Deleter(amazonS3Client)

  override def listObjects(
      bucket: Bucket,
      prefix: RemoteKey
  ): TaskR[Console, S3ObjectsData] =
    objectLister.listObjects(bucket, prefix)

  override def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): UIO[StorageQueueEvent] =
    copier.copy(bucket, sourceKey, hash, targetKey)

  override def upload(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): Task[StorageQueueEvent] =
    uploader.upload(localFile, bucket, batchMode, uploadEventListener, 1)

  override def delete(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): UIO[StorageQueueEvent] =
    deleter.delete(bucket, remoteKey)

  override def shutdown: UIO[StorageQueueEvent] = {
    amazonTransferManager.shutdownNow(true)
    amazonS3Client.shutdown().map(_ => ShutdownQueueEvent())
  }

}
