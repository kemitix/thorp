package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.storage.Storage.Service
import net.kemitix.thorp.uishell.UploadEventListener
import zio.{RIO, UIO}

object S3Storage {
  trait Live extends Storage {
    val storage: Service = new Service {

      private val client: AmazonS3Client =
        AmazonS3Client.create(AmazonS3ClientBuilder.standard().build())
      private val transferManager: S3TransferManager =
        S3TransferManager.create(TransferManagerBuilder.defaultTransferManager)
      private val copier   = S3Copier.copier(client)
      private val uploader = S3Uploader.uploader(transferManager)
      private val deleter  = S3Deleter.deleter(client)
      private val lister   = S3Lister.lister(client)

      override def listObjects(
          bucket: Bucket,
          prefix: RemoteKey): RIO[Storage with Console, RemoteObjects] =
        UIO {
          lister(S3Lister.request(bucket, prefix))
        }

      override def upload(
          localFile: LocalFile,
          bucket: Bucket,
          listenerSettings: UploadEventListener.Settings,
      ): UIO[StorageEvent] =
        UIO {
          uploader(S3Uploader.request(localFile, bucket))
        }

      override def copy(bucket: Bucket,
                        sourceKey: RemoteKey,
                        hash: MD5Hash,
                        targetKey: RemoteKey): UIO[StorageEvent] =
        UIO {
          copier(S3Copier.request(bucket, sourceKey, hash, targetKey))
        }

      override def delete(bucket: Bucket,
                          remoteKey: RemoteKey): UIO[StorageEvent] =
        UIO {
          deleter(S3Deleter.request(bucket, remoteKey))
        }

      override def shutdown: UIO[StorageEvent] = {
        UIO(transferManager.shutdownNow(true)) *> UIO(client.shutdown())
          .map(_ => StorageEvent.shutdownEvent())
      }
    }
  }
  object Live extends Live
}
