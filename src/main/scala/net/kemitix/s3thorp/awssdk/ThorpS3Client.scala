package net.kemitix.s3thorp.awssdk

import java.io.File

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, LastModified, MD5Hash, RemoteKey}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model._

import scala.collection.JavaConverters._

private class ThorpS3Client(s3Client: S3CatsIOClient) extends S3Client {

  override def upload(localFile: File,
                      bucket: Bucket,
                      remoteKey: RemoteKey
                     ): IO[Either[Throwable, MD5Hash]] = {
    val request = PutObjectRequest.builder()
      .bucket(bucket.name)
      .key(remoteKey.key)
      .build()
    val body = AsyncRequestBody.fromFile(localFile)
    s3Client.putObject(request, body).map(r => Right(MD5Hash(r.eTag())))
  }

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey
                   ): IO[Either[Throwable, RemoteKey]] = {
    val request = CopyObjectRequest.builder()
      .bucket(bucket.name)
      .copySource(s"$bucket/$sourceKey")
      .copySourceIfMatch(hash.hash)
      .key(targetKey.key)
      .build()
    s3Client.copyObject(request)
      .map(_ => Right(targetKey))
  }

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey
                     ): IO[Either[Throwable, RemoteKey]] = {
    val request = DeleteObjectRequest.builder()
      .bucket(bucket.name)
      .key(remoteKey.key)
      .build()
    s3Client.deleteObject(request)
      .map(_ => Right(remoteKey))
  }

  private def asHashLookup: Stream[S3Object] => HashLookup =
    os => HashLookup(byHash(os), byKey(os))

  private def byHash(os: Stream[S3Object]) =
    os.map{o => (MD5Hash(o.eTag), (RemoteKey(o.key), LastModified(o.lastModified)))}.toMap

  private def byKey(os: Stream[S3Object]) =
    os.map{o => (RemoteKey(o.key()), (MD5Hash(o.eTag()), LastModified(o.lastModified())))}.toMap

  def listObjects(bucket: Bucket, prefix: RemoteKey): IO[HashLookup] = {
    val request = ListObjectsV2Request.builder()
      .bucket(bucket.name)
      .prefix(prefix.key)
      .build()
    s3Client.listObjectsV2(request)
      .map(r => r.contents.asScala.toStream)
      .map(asHashLookup)
  }

}
