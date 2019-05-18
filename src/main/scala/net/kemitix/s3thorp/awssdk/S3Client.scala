package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._

trait S3Client {

  final def getS3Status(localFile: LocalFile)
                       (implicit s3ObjectsData: S3ObjectsData): (Option[HashModified], Set[KeyModified]) = {
    val matchingByKey = s3ObjectsData.byKey.get(localFile.remoteKey)
    val matchingByHash = s3ObjectsData.byHash.getOrElse(localFile.hash, Set())
    (matchingByKey, matchingByHash)
  }

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