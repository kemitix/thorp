package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._
import software.amazon.awssdk.services.s3.model.{Bucket => _, _}

private class ThorpS3Client(s3Client: S3CatsIOClient)
  extends S3Client
    with S3ClientLogging
    with QuoteStripper {

  lazy val objectLister = new S3ClientObjectLister(s3Client)
  lazy val copier = new S3ClientCopier(s3Client)
  lazy val uploader = new S3ClientUploader(s3Client)
  lazy val multiPartUploader = new S3ClientMultiPartUploader(s3Client)
  lazy val deleter = new S3ClientDeleter(s3Client)

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
                      bucket: Bucket)
                     (implicit c: Config): IO[UploadS3Action] =
    if (multiPartUploader.accepts(localFile)) multiPartUploader.upload(localFile, bucket)
    else uploader.upload(localFile, bucket)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit c: Config): IO[DeleteS3Action] =
    deleter.delete(bucket, remoteKey)

}
