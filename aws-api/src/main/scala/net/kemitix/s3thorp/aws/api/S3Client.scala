package net.kemitix.s3thorp.aws.api

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, MD5Hash, RemoteKey, S3ObjectsData}

trait S3Client {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey
                 )(implicit info: Int => String => IO[Unit]): IO[S3ObjectsData]

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener[IO],
             multiPartThreshold: Long,
             tryCount: Int,
             maxRetries: Int)
            (implicit info: Int => String => IO[Unit],
             warn: String => IO[Unit]): IO[S3Action]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey
          )(implicit info: Int => String => IO[Unit]): IO[CopyS3Action]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey
            )(implicit info: Int => String => IO[Unit]): IO[DeleteS3Action]

}
