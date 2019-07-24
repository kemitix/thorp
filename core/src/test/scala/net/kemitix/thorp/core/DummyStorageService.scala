package net.kemitix.thorp.core

import java.io.File

import net.kemitix.thorp.console._
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.StorageService
import zio.{Task, TaskR}

case class DummyStorageService(s3ObjectData: S3ObjectsData,
                               uploadFiles: Map[File, (RemoteKey, MD5Hash)])
    extends StorageService {

  override def shutdown: Task[StorageQueueEvent] =
    Task(StorageQueueEvent.ShutdownQueueEvent())

  override def listObjects(
      bucket: Bucket,
      prefix: RemoteKey
  ): TaskR[Console, S3ObjectsData] =
    TaskR(s3ObjectData)

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      batchMode: Boolean,
                      uploadEventListener: UploadEventListener,
                      tryCount: Int): Task[StorageQueueEvent] = {
    val (remoteKey, md5Hash) = uploadFiles(localFile.file)
    Task(StorageQueueEvent.UploadQueueEvent(remoteKey, md5Hash))
  }

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey): Task[StorageQueueEvent] =
    Task(StorageQueueEvent.CopyQueueEvent(sourceKey, targetKey))

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey): Task[StorageQueueEvent] =
    Task(StorageQueueEvent.DeleteQueueEvent(remoteKey))

}
