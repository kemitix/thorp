package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action}
import net.kemitix.s3thorp.aws.api.{S3Action, S3Client, UploadProgressListener}
import net.kemitix.s3thorp.domain._

class ThorpS3Client(amazonS3Client: => AmazonS3,
                    amazonS3TransferManager: => TransferManager)
  extends S3Client {

  lazy val objectLister = new S3ClientObjectLister(amazonS3Client)
  lazy val copier = new S3ClientCopier[IO](amazonS3Client)
  lazy val uploader = new Uploader(amazonS3TransferManager)
  lazy val deleter = new S3ClientDeleter(amazonS3Client)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => IO[Unit]): IO[S3ObjectsData] =
    objectLister.listObjects(bucket, prefix)

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey)
                   (implicit info: Int => String => IO[Unit]): IO[CopyS3Action] =
    copier.copy(bucket, sourceKey,hash, targetKey)

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      multiPartThreshold: Long,
                      tryCount: Int,
                      maxRetries: Int)
                     (implicit info: Int => String => IO[Unit],
                      warn: String => IO[Unit]): IO[S3Action] =
    uploader.upload(localFile, bucket, progressListener, multiPartThreshold, 1, maxRetries)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit info: Int => String => IO[Unit]): IO[DeleteS3Action] =
    deleter.delete(bucket, remoteKey)

}
