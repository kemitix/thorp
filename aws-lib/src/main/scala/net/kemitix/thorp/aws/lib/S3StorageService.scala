package net.kemitix.thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{StorageQueueEvent, StorageService}

class S3StorageService(amazonS3Client: => AmazonS3,
                       amazonS3TransferManager: => TransferManager)
  extends StorageService {

  lazy val objectLister = new S3ClientObjectLister(amazonS3Client)
  lazy val copier = new S3ClientCopier(amazonS3Client)
  lazy val uploader = new Uploader(amazonS3TransferManager)
  lazy val deleter = new S3ClientDeleter(amazonS3Client)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit logger: Logger): IO[S3ObjectsData] =
    objectLister.listObjects(bucket, prefix)

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey)
                   (implicit logger: Logger): IO[StorageQueueEvent] =
    copier.copy(bucket, sourceKey,hash, targetKey)

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      uploadEventListener: UploadEventListener,
                      tryCount: Int)
                     (implicit logger: Logger): IO[StorageQueueEvent] =
    uploader.upload(localFile, bucket, uploadEventListener, 1)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit logger: Logger): IO[StorageQueueEvent] =
    deleter.delete(bucket, remoteKey)

}
