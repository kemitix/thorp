package net.kemitix.thorp.storage.aws

import cats.data.EitherT
import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.thorp.domain.StorageQueueEvent.ShutdownQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.StorageService

class S3StorageService(amazonS3Client: => AmazonS3,
                       amazonS3TransferManager: => TransferManager)
  extends StorageService {

  lazy val objectLister = new Lister(amazonS3Client)
  lazy val copier = new Copier(amazonS3Client)
  lazy val uploader = new Uploader(amazonS3TransferManager)
  lazy val deleter = new Deleter(amazonS3Client)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey): EitherT[IO, String, S3ObjectsData] =
    objectLister.listObjects(bucket, prefix)

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey): IO[StorageQueueEvent] =
    copier.copy(bucket, sourceKey,hash, targetKey)

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      uploadEventListener: UploadEventListener,
                      tryCount: Int): IO[StorageQueueEvent] =
    uploader.upload(localFile, bucket, uploadEventListener, 1)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey): IO[StorageQueueEvent] =
    deleter.delete(bucket, remoteKey)

  override def shutdown: IO[StorageQueueEvent] =
    IO {
      amazonS3TransferManager.shutdownNow(true)
      amazonS3Client.shutdown()
      ShutdownQueueEvent()
    }

}
