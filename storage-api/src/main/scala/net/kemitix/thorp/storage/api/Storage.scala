package net.kemitix.thorp.storage.api

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import zio.{Task, RIO, UIO, ZIO}

trait Storage {
  val storage: Storage.Service
}

object Storage {
  trait Service {

    def listObjects(
        bucket: Bucket,
        prefix: RemoteKey
    ): RIO[Storage with Console, RemoteObjects]

    def upload(
        localFile: LocalFile,
        bucket: Bucket,
        listenerSettings: UploadEventListener.Settings,
    ): ZIO[Storage with Config, Nothing, StorageQueueEvent]

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

    def listResult: Task[RemoteObjects]
    def uploadResult: UIO[StorageQueueEvent]
    def copyResult: UIO[StorageQueueEvent]
    def deleteResult: UIO[StorageQueueEvent]
    def shutdownResult: UIO[StorageQueueEvent]

    val storage: Service = new Service {

      override def listObjects(
          bucket: Bucket,
          prefix: RemoteKey): RIO[Storage with Console, RemoteObjects] =
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

  object Test extends Test {
    override def listResult: Task[RemoteObjects] =
      Task.die(new NotImplementedError)
    override def uploadResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
    override def copyResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
    override def deleteResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
    override def shutdownResult: UIO[StorageQueueEvent] =
      Task.die(new NotImplementedError)
  }

  final def list(bucket: Bucket,
                 prefix: RemoteKey): RIO[Storage with Console, RemoteObjects] =
    ZIO.accessM(_.storage listObjects (bucket, prefix))

  final def upload(
      localFile: LocalFile,
      bucket: Bucket,
      listenerSettings: UploadEventListener.Settings
  ): ZIO[Storage with Config, Nothing, StorageQueueEvent] =
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
