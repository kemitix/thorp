package net.kemitix.thorp.storage.api

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import zio.{TaskR, UIO}

trait Storage {
  val storage: Storage.Service
}

object Storage {
  trait Service {

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
    ): UIO[StorageQueueEvent]

    def copy(
        bucket: Bucket,
        sourceKey: RemoteKey,
        hash: MD5Hash,
        targetKey: RemoteKey
    ): UIO[StorageQueueEvent]

    def delete(
        bucket: Bucket,
        remoteKey: RemoteKey
    ): UIO[StorageQueueEvent]

    def shutdown: UIO[StorageQueueEvent]
  }
}