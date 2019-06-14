package net.kemitix.s3thorp.aws.api

import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, MD5Hash, RemoteKey, S3ObjectsData}

trait S3Client[M[_]] {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey
                 )(implicit info: Int => String => M[Unit]): M[S3ObjectsData]

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             multiPartThreshold: Long,
             tryCount: Int,
             maxRetries: Int)
            (implicit info: Int => String => M[Unit],
             warn: String => M[Unit]): M[S3Action]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey
          )(implicit info: Int => String => M[Unit]): M[CopyS3Action]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey
            )(implicit info: Int => String => M[Unit]): M[DeleteS3Action]

}
