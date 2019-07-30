package net.kemitix.thorp.storage.api

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import zio.{Task, TaskR, UIO, ZIO}

trait Storage {
  val storage: Storage.Service
}

object Storage {
  trait Service {

    def listObjects(
        bucket: Bucket,
        prefix: RemoteKey
    ): TaskR[Storage with Console, S3ObjectsData]

    def upload(
        localFile: LocalFile,
        bucket: Bucket,
        uploadEventListener: UploadEventListener,
        tryCount: Int
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

    def listResult: Task[S3ObjectsData]
    def uploadResult: UIO[StorageQueueEvent]
    def copyResult: UIO[StorageQueueEvent]
    def deleteResult: UIO[StorageQueueEvent]
    def shutdownResult: UIO[StorageQueueEvent]

    val storage: Service = new Service {

      override def listObjects(
          bucket: Bucket,
          prefix: RemoteKey): TaskR[Storage with Console, S3ObjectsData] =
        listResult

      override def upload(
          localFile: LocalFile,
          bucket: Bucket,
          uploadEventListener: UploadEventListener,
          tryCount: Int): ZIO[Storage, Nothing, StorageQueueEvent] =
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
    override def listResult: Task[S3ObjectsData] =
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

  final def list(
      bucket: Bucket,
      prefix: RemoteKey): TaskR[Storage with Console, S3ObjectsData] =
    ZIO.accessM(_.storage listObjects (bucket, prefix))

  final def upload(
      localFile: LocalFile,
      bucket: Bucket,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): ZIO[Storage with Config, Nothing, StorageQueueEvent] =
    ZIO.accessM(
      _.storage upload (localFile, bucket, uploadEventListener, tryCount))

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
