package net.kemitix.s3thorp.awssdk

import java.io.File

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, LastModified, MD5Hash, RemoteKey, UploadS3Action}

trait S3Client {

  final def objectHead(remoteKey: RemoteKey)
                      (implicit hashLookup: HashLookup): Option[(MD5Hash, LastModified)] =
    hashLookup.byKey.get(remoteKey)

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey
                 ): IO[HashLookup]

  def upload(localFile: File,
             bucket: Bucket,
             remoteKey: RemoteKey
            ): IO[UploadS3Action]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey
          ): IO[Either[Throwable, RemoteKey]]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey
            ): IO[Either[Throwable, RemoteKey]]

}

object S3Client {

  val defaultClient: S3Client =
    new ThorpS3Client(
      S3CatsIOClient(new JavaClientWrapper {}.underlying))

}