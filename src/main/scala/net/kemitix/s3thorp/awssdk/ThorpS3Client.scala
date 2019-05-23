package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{Bucket => _, _}

import scala.collection.JavaConverters._

private class ThorpS3Client(s3Client: S3CatsIOClient)
  extends S3Client
    with S3ObjectsByHash
    with S3ClientLogging {

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit c: Config): IO[S3ObjectsData] = {
    val request = ListObjectsV2Request.builder
      .bucket(bucket.name)
      .prefix(prefix.key).build
    s3Client.listObjectsV2(request)
      .bracket(
        logListObjectsStart(bucket, prefix))(
        logListObjectsFinish(bucket,prefix))
      .map(_.contents)
      .map(_.asScala)
      .map(_.toStream)
      .map(os => S3ObjectsData(byHash(os), byKey(os)))
  }

  private def byKey(os: Stream[S3Object]) =
    os.map { o => {
      val remoteKey = RemoteKey(o.key)
      val hash = MD5Hash(o.eTag() filter stripQuotes)
      val lastModified = LastModified(o.lastModified())
      (remoteKey, HashModified(hash, lastModified))
    }}.toMap

  override def upload(localFile: LocalFile,
                      bucket: Bucket)
                     (implicit c: Config): IO[UploadS3Action] = {
    val request = PutObjectRequest.builder
      .bucket(bucket.name)
      .key(localFile.remoteKey.key).build
    val body = AsyncRequestBody.fromFile(localFile.file)
    s3Client.putObject(request, body)
      .bracket(
        logUploadStart(localFile, bucket))(
        logUploadFinish(localFile, bucket))
      .map(_.eTag)
      .map(_ filter stripQuotes)
      .map(MD5Hash)
      .map(UploadS3Action(localFile.remoteKey, _))
  }

  private def stripQuotes: Char => Boolean = _ != '"'

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey)
                   (implicit c: Config): IO[CopyS3Action] = {
    val request = CopyObjectRequest.builder
      .bucket(bucket.name)
      .copySource(s"${bucket.name}/${sourceKey.key}")
      .copySourceIfMatch(hash.hash)
      .key(targetKey.key).build
    s3Client.copyObject(request)
      .bracket(
        logCopyStart(bucket, sourceKey, targetKey))(
        logCopyFinish(bucket, sourceKey,targetKey))
      .map(_ => CopyS3Action(targetKey))
  }

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit c: Config): IO[DeleteS3Action] = {
    val request = DeleteObjectRequest.builder
      .bucket(bucket.name)
      .key(remoteKey.key).build
    s3Client.deleteObject(request)
      .bracket(
        logDeleteStart(bucket, remoteKey))(
        logDeleteFinish(bucket, remoteKey))
      .map(_ => DeleteS3Action(remoteKey))
  }

}
