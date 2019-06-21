package net.kemitix.thorp.storage.api

import cats.effect.IO
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.S3Action.{CopyS3Action, DeleteS3Action}

trait StorageService {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey
                 )(implicit logger: Logger): IO[S3ObjectsData]

  def upload(localFile: LocalFile,
             bucket: Bucket,
             uploadProgressListener: UploadProgressListener,
             tryCount: Int)
            (implicit logger: Logger): IO[S3Action]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey
          )(implicit logger: Logger): IO[CopyS3Action]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey
            )(implicit logger: Logger): IO[DeleteS3Action]

}
