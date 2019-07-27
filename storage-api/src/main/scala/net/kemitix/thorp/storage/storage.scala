package net.kemitix.thorp

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.Storage
import zio.{TaskR, ZIO}

package object storage {

  final val storageService: ZIO[Storage, Nothing, Storage.Service] =
    ZIO.access(_.storage)

  final def listObjects(
      bucket: Bucket,
      prefix: RemoteKey): TaskR[Storage with Console, S3ObjectsData] =
    ZIO.accessM(_.storage listObjects (bucket, prefix))

  final def upload(
      localFile: LocalFile,
      bucket: Bucket,
      batchMode: Boolean,
      uploadEventListener: UploadEventListener,
      tryCount: Int
  ): ZIO[Storage, Nothing, StorageQueueEvent] =
    ZIO.accessM(
      _.storage upload (localFile, bucket, batchMode, uploadEventListener, tryCount))

  final def copyObject(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey
  ): ZIO[Storage, Nothing, StorageQueueEvent] =
    ZIO.accessM(_.storage copy (bucket, sourceKey, hash, targetKey))

  final def deleteObject(
      bucket: Bucket,
      remoteKey: RemoteKey
  ): ZIO[Storage, Nothing, StorageQueueEvent] =
    ZIO.accessM(_.storage delete (bucket, remoteKey))

}
