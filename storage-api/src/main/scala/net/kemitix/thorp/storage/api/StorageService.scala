package net.kemitix.thorp.storage.api

import cats.data.EitherT
import cats.effect.IO
import net.kemitix.thorp.domain._

trait StorageService {

  def shutdown: IO[StorageQueueEvent]

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey): EitherT[IO, String, S3ObjectsData]

  def upload(localFile: LocalFile,
             bucket: Bucket,
             batchMode: Boolean,
             uploadEventListener: UploadEventListener,
             tryCount: Int): IO[StorageQueueEvent]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey): IO[StorageQueueEvent]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey): IO[StorageQueueEvent]

}
