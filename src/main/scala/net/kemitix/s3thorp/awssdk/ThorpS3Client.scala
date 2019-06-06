package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.aws.api.{S3Action, S3Client, UploadProgressListener}
import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action}
import net.kemitix.s3thorp.domain._
import software.amazon.awssdk.services.s3.model.{Bucket => _}

class ThorpS3Client(ioS3Client: S3CatsIOClient,
                    amazonS3Client: => AmazonS3,
                    amazonS3TransferManager: => TransferManager)
  extends S3Client
    with S3ClientLogging
    with QuoteStripper {

//  lazy val amazonS3Client = AmazonS3ClientBuilder.defaultClient
//  lazy val amazonS3TransferManager = TransferManagerBuilder.defaultTransferManager
  lazy val objectLister = new S3ClientObjectLister(ioS3Client)
  lazy val copier = new S3ClientCopier(ioS3Client)
  lazy val uploader = new S3ClientPutObjectUploader(amazonS3Client)
  lazy val multiPartUploader = new S3ClientMultiPartTransferManager(amazonS3TransferManager)
  lazy val deleter = new S3ClientDeleter(ioS3Client)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => Unit): IO[S3ObjectsData] =
    objectLister.listObjects(bucket, prefix)


  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey)
                   (implicit info: Int => String => Unit): IO[CopyS3Action] =
    copier.copy(bucket, sourceKey,hash, targetKey)


  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      multiPartThreshold: Long,
                      tryCount: Int,
                      maxRetries: Int)
                     (implicit info: Int => String => Unit,
                      warn: String => Unit): IO[S3Action] =
    if (multiPartUploader.accepts(localFile)(multiPartThreshold))
      multiPartUploader.upload(localFile, bucket, progressListener, multiPartThreshold, 1, maxRetries)
    else
      uploader.upload(localFile, bucket, progressListener, multiPartThreshold, tryCount, maxRetries)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit info: Int => String => Unit): IO[DeleteS3Action] =
    deleter.delete(bucket, remoteKey)

}
