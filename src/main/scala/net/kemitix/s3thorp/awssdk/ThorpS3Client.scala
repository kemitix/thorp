package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._
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
                          (implicit c: Config): IO[S3ObjectsData] =
    objectLister.listObjects(bucket, prefix)


  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey)
                   (implicit c: Config): IO[CopyS3Action] =
    copier.copy(bucket, sourceKey,hash, targetKey)


  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      tryCount: Int)
                     (implicit c: Config): IO[S3Action] =

    if (multiPartUploader.accepts(localFile)) multiPartUploader.upload(localFile, bucket, progressListener, 1)
    else uploader.upload(localFile, bucket, progressListener, tryCount)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit c: Config): IO[DeleteS3Action] =
    deleter.delete(bucket, remoteKey)

}
