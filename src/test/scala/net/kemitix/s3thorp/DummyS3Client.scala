package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.S3Action.{CopyS3Action, DeleteS3Action, UploadS3Action}
import net.kemitix.s3thorp.awssdk.{S3Client, UploadProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, Config, LocalFile, MD5Hash, RemoteKey, S3ObjectsData}

trait DummyS3Client extends S3Client {

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      multiPartThreshold: Long,
                      tryCount: Int,
                      maxRetries: Int)
                     (implicit info: Int => String => Unit,
                      warn: String => Unit): IO[UploadS3Action] = ???

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey
                   )(implicit info: Int => String => Unit): IO[CopyS3Action] = ???

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey
                     )(implicit info: Int => String => Unit): IO[DeleteS3Action] = ???

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey
                          )(implicit info: Int => String => Unit): IO[S3ObjectsData] = ???

}
