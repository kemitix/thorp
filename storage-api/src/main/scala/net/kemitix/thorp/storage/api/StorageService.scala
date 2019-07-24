package net.kemitix.thorp.storage.api

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import zio.{Task, TaskR}

trait StorageService {

  def shutdown: Task[StorageQueueEvent]

  def listObjects(
      bucket: Bucket,
      prefix: RemoteKey
  ): TaskR[Console, S3ObjectsData]

  def upload(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): Task[StorageQueueEvent]

  def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): Task[StorageQueueEvent]

  def delete(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): Task[StorageQueueEvent]

}
