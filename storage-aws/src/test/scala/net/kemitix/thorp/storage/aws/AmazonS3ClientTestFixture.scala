package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.UploadEventListener
import org.scalamock.scalatest.MockFactory
import zio.{RIO, UIO}

trait AmazonS3ClientTestFixture extends MockFactory {

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  private val manager = stub[AmazonTransferManager]
  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  private val client   = stub[AmazonS3Client]
  val fixture: Fixture = Fixture(client, manager)

  case class Fixture(
      amazonS3Client: AmazonS3Client,
      amazonS3TransferManager: AmazonTransferManager,
  ) {
    lazy val storageService: Storage.Service =
      new Storage.Service {

        private val client          = amazonS3Client
        private val transferManager = amazonS3TransferManager

        override def listObjects(
            bucket: Bucket,
            prefix: RemoteKey
        ): RIO[Storage with Console, RemoteObjects] =
          Lister.listObjects(client)(bucket, prefix)

        override def upload(
            localFile: LocalFile,
            bucket: Bucket,
            listenerSettings: UploadEventListener.Settings,
        ): UIO[StorageEvent] =
          Uploader.upload(transferManager)(
            Uploader.Request(localFile, bucket, listenerSettings))

        override def copy(
            bucket: Bucket,
            sourceKey: RemoteKey,
            hash: MD5Hash,
            targetKey: RemoteKey
        ): UIO[StorageEvent] =
          UIO {
            val request = S3Copier.request(bucket, sourceKey, hash, targetKey)
            S3Copier.copier(client)(request)
          }

        override def delete(
            bucket: Bucket,
            remoteKey: RemoteKey
        ): UIO[StorageEvent] =
          Deleter.delete(client)(bucket, remoteKey)

        override def shutdown: UIO[StorageEvent] = {
          transferManager.shutdownNow(true) *>
            UIO(client.shutdown()).map(_ => StorageEvent.shutdownEvent())
        }
      }
  }

}
