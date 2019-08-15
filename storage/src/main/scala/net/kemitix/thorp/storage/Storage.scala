package net.kemitix.thorp.storage

import net.kemitix.thorp.domain._
import zio.{RIO, Task, UIO, ZIO}

trait Storage {
  val storage: Storage.Service
}

object Storage {
  trait Service {

    def listObjects(
        bucket: Bucket,
        prefix: RemoteKey
    ): RIO[Storage, RemoteObjects]

    def upload(
        localFile: LocalFile,
        bucket: Bucket,
        listenerSettings: UploadEventListener.Settings,
    ): ZIO[Storage, Nothing, StorageQueueEvent]

    def copy(
        bucket: Bucket,
        sourceKey: RemoteKey,
        hash: MD5Hash,
        targetKey: RemoteKey
    ): ZIO[Storage, Nothing, StorageQueueEvent]

    def delete(
        bucket: Bucket,
        remoteKey: RemoteKey
    ): UIO[StorageQueueEvent]

    def shutdown: UIO[StorageQueueEvent]
  }

  trait Test extends Storage {

    def listResult: Task[RemoteObjects] =
      Task.die(new NotImplementedError)
    def uploadResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
    def copyResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
    def deleteResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
    def shutdownResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)

    val storage: Service = new Service {

      override def listObjects(bucket: Bucket,
                               prefix: RemoteKey): RIO[Storage, RemoteObjects] =
        listResult

      override def upload(
          localFile: LocalFile,
          bucket: Bucket,
          listenerSettings: UploadEventListener.Settings
      ): ZIO[Storage, Nothing, StorageQueueEvent] =
        uploadResult

      override def copy(
          bucket: Bucket,
          sourceKey: RemoteKey,
          hash: MD5Hash,
          targetKey: RemoteKey): ZIO[Storage, Nothing, StorageQueueEvent] =
        copyResult

      override def delete(bucket: Bucket,
                          remoteKey: RemoteKey): UIO[StorageQueueEvent] =
        deleteResult

      override def shutdown: UIO[StorageQueueEvent] =
        shutdownResult

    }
  }

  object Test extends Test

  final def list(bucket: Bucket,
                 prefix: RemoteKey): RIO[Storage, RemoteObjects] =
    ZIO.accessM(_.storage listObjects (bucket, prefix))

  final def upload(
      localFile: LocalFile,
      bucket: Bucket,
      listenerSettings: UploadEventListener.Settings
  ): ZIO[Storage, Nothing, StorageQueueEvent] =
    ZIO.accessM(_.storage upload (localFile, bucket, listenerSettings))

  final def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): ZIO[Storage, Nothing, StorageQueueEvent] =
    ZIO.accessM(_.storage copy (bucket, sourceKey, hash, targetKey))

  final def delete(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): ZIO[Storage, Nothing, StorageQueueEvent] =
    ZIO.accessM(_.storage delete (bucket, remoteKey))

}
