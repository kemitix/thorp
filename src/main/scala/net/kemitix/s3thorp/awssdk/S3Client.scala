package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.Sync.{Bucket, LastModified, LocalFile, MD5Hash, RemoteKey}

trait S3Client {

  final def objectHead(remoteKey: RemoteKey)(implicit hashLookup: HashLookup): Option[(MD5Hash, LastModified)] =
    hashLookup.byKey.get(remoteKey)

  def listObjects(bucket: Bucket, prefix: RemoteKey): IO[HashLookup]

  def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Either[Throwable, MD5Hash]]

}

object S3Client {

  val defaultClient: S3Client =
    new ThorpS3Client(
      S3CatsIOClient(new JavaClientWrapper {}.underlying))

}