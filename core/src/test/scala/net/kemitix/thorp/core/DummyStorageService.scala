package net.kemitix.thorp.core

import java.io.File

import cats.data.EitherT
import cats.effect.IO
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.StorageService

case class DummyStorageService(s3ObjectData: S3ObjectsData,
                               uploadFiles: Map[File, (RemoteKey, MD5Hash)])
  extends StorageService {

  override def shutdown: IO[StorageQueueEvent] =
    IO.pure(StorageQueueEvent.ShutdownQueueEvent())

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit l: Logger): EitherT[IO, String, S3ObjectsData] =
    EitherT.liftF(IO.pure(s3ObjectData))

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      batchMode: Boolean,
                      uploadEventListener: UploadEventListener,
                      tryCount: Int): IO[StorageQueueEvent] = {
    val (remoteKey, md5Hash) = uploadFiles(localFile.file)
    IO.pure(StorageQueueEvent.UploadQueueEvent(remoteKey, md5Hash))
  }

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey): IO[StorageQueueEvent] =
    IO.pure(StorageQueueEvent.CopyQueueEvent(targetKey))

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey): IO[StorageQueueEvent] =
    IO.pure(StorageQueueEvent.DeleteQueueEvent(remoteKey))

}
