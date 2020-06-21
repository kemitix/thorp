package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.UploadEventListener
import org.scalamock.scalatest.MockFactory
import zio.{RIO, UIO}

trait AmazonS3ClientTestFixture extends MockFactory {

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  private val manager = stub[S3TransferManager]
  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  private val client = stub[AmazonS3Client]
  val fixture: Fixture = Fixture(client, manager)

  case class Fixture(amazonS3Client: AmazonS3Client,
                     amazonS3TransferManager: S3TransferManager,
  ) {
    lazy val storageService: Storage.Service =
      new Storage.Service {

        private val client = amazonS3Client
        private val transferManager = amazonS3TransferManager

        override def listObjects(
          bucket: Bucket,
          prefix: RemoteKey
        ): RIO[Storage, RemoteObjects] =
          UIO {
            S3Lister.lister(client)(S3Lister.request(bucket, prefix))
          }

        override def upload(localFile: LocalFile,
                            bucket: Bucket,
                            listenerSettings: UploadEventListener.Settings,
        ): UIO[StorageEvent] =
          UIO(
            S3Uploader
              .uploader(transferManager)(S3Uploader.request(localFile, bucket))
          )

        override def copy(bucket: Bucket,
                          sourceKey: RemoteKey,
                          hash: MD5Hash,
                          targetKey: RemoteKey): UIO[StorageEvent] =
          UIO {
            val request = S3Copier.request(bucket, sourceKey, hash, targetKey)
            S3Copier.copier(client)(request)
          }

        override def delete(bucket: Bucket,
                            remoteKey: RemoteKey): UIO[StorageEvent] =
          UIO(S3Deleter.deleter(client)(S3Deleter.request(bucket, remoteKey)))

        override def shutdown: UIO[StorageEvent] = {
          UIO(transferManager.shutdownNow(true)) *> UIO(client.shutdown())
            .map(_ => StorageEvent.shutdownEvent())
        }
      }
  }

}
