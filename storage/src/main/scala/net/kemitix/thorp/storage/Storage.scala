package net.kemitix.thorp.storage

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import net.kemitix.thorp.uishell.UploadEventListener
import zio.{RIO, Task, UIO, ZIO}

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
    ): ZIO[Storage, Nothing, StorageEvent]

    def copy(
        bucket: Bucket,
        sourceKey: RemoteKey,
        hash: MD5Hash,
        targetKey: RemoteKey
    ): ZIO[Storage, Nothing, StorageEvent]

    def delete(
        bucket: Bucket,
        remoteKey: RemoteKey
    ): UIO[StorageEvent]

    def shutdown: UIO[StorageEvent]
  }

  trait Test extends Storage {

    def listResult: Task[RemoteObjects] =
      Task.die(new NotImplementedError)
    def uploadResult: UIO[StorageEvent] =
      Task.die(new NotImplementedError)
    def copyResult: UIO[StorageEvent] =
      Task.die(new NotImplementedError)
    def deleteResult: UIO[StorageEvent] =
      Task.die(new NotImplementedError)
    def shutdownResult: UIO[StorageEvent] =
      Task.die(new NotImplementedError)

    val storage: Service = new Service {

      override def listObjects(bucket: Bucket,
                               prefix: RemoteKey): RIO[Storage, RemoteObjects] =
        listResult

      override def upload(
          localFile: LocalFile,
          bucket: Bucket,
          listenerSettings: UploadEventListener.Settings
      ): ZIO[Storage, Nothing, StorageEvent] =
        uploadResult

      override def copy(
          bucket: Bucket,
          sourceKey: RemoteKey,
          hash: MD5Hash,
          targetKey: RemoteKey): ZIO[Storage, Nothing, StorageEvent] =
        copyResult

      override def delete(bucket: Bucket,
                          remoteKey: RemoteKey): UIO[StorageEvent] =
        deleteResult

      override def shutdown: UIO[StorageEvent] =
        shutdownResult

    }
  }

  object Test extends Test

  final def list(bucket: Bucket,
                 prefix: RemoteKey): RIO[Storage with Console, RemoteObjects] =
    ZIO.accessM(_.storage listObjects (bucket, prefix))

  final def upload(
      localFile: LocalFile,
      bucket: Bucket,
      listenerSettings: UploadEventListener.Settings
  ): ZIO[Storage, Nothing, StorageEvent] =
    ZIO.accessM(_.storage upload (localFile, bucket, listenerSettings))

  final def copy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): ZIO[Storage, Nothing, StorageEvent] =
    ZIO.accessM(_.storage copy (bucket, sourceKey, hash, targetKey))

  final def delete(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): ZIO[Storage, Nothing, StorageEvent] =
    ZIO.accessM(_.storage delete (bucket, remoteKey))

}
