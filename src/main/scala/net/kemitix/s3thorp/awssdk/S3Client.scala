package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
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
                 )(implicit c: Config): IO[S3ObjectsData]

  def upload(localFile: LocalFile,
             bucket: Bucket,
             tryCount: Int
            )(implicit c: Config): IO[S3Action]

  def copy(bucket: Bucket,
           sourceKey: RemoteKey,
           hash: MD5Hash,
           targetKey: RemoteKey
          )(implicit c: Config): IO[CopyS3Action]

  def delete(bucket: Bucket,
             remoteKey: RemoteKey
            )(implicit c: Config): IO[DeleteS3Action]

}

object S3Client {

  def createClient(s3AsyncClient: S3AsyncClient): S3Client = {
    new ThorpS3Client(S3CatsIOClient(s3AsyncClient))
  }

  val defaultClient: S3Client =
    createClient(new JavaClientWrapper {}.underlying)

}