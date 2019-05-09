package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Bucket, MD5Hash, LastModified, LocalFile, RemoteKey}

trait S3Client {

  def objectHead(bucket: Bucket, remoteKey: RemoteKey): IO[Option[(MD5Hash, LastModified)]]

  def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Unit]

}

object S3Client {

  val defaultClient: S3Client = new ReactiveS3Client

}