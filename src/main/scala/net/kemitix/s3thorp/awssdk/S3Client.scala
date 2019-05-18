package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._

trait S3Client {

  final def objectHead(remoteKey: RemoteKey)
                      (implicit s3ObjectsData: S3ObjectsData): Option[HashModified] =
    s3ObjectsData.byKey.get(remoteKey)

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey
                 ): IO[S3ObjectsData]

  def upload(localFile: LocalFile,
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