package net.kemitix.s3thorp.aws.api

import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action}
import net.kemitix.thorp.domain._

trait S3Client[M[_]] {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey
                 )(implicit logger: Logger[M]): M[S3ObjectsData]

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             multiPartThreshold: Long,
             tryCount: Int,
             maxRetries: Int)
            (implicit logger: Logger[M]): M[S3Action]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey
          )(implicit logger: Logger[M]): M[CopyS3Action]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey
            )(implicit logger: Logger[M]): M[DeleteS3Action]

}
