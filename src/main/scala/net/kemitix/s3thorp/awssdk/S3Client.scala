package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.Sync.{Bucket, LastModified, LocalFile, MD5Hash, RemoteKey}

trait S3Client {

  def objectHead(bucket: Bucket, remoteKey: RemoteKey): IO[Option[(MD5Hash, LastModified)]]

  def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Either[Throwable, MD5Hash]]

}

object S3Client {

  val defaultClient: S3Client =
    new ThropS3Client(
      S3CatsIOClient(new JavaClientWrapper {}.underlying))

}