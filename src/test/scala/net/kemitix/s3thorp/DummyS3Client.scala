package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.Sync.LocalFile
import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait DummyS3Client extends S3Client {

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      remoteKey: RemoteKey
                     ): IO[Either[Throwable, MD5Hash]] = ???

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey
                   ): IO[Either[Throwable, RemoteKey]] = ???

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey
                     ): IO[Either[Throwable, RemoteKey]] = ???

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey
                          ): IO[HashLookup] = ???

}
