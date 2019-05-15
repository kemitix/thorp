package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.Sync._
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, PutObjectRequest, S3Object}

import scala.collection.JavaConverters._

private class ThorpS3Client(s3Client: S3CatsIOClient) extends S3Client {

  def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Either[Throwable, MD5Hash]] = {
    val request = PutObjectRequest.builder()
      .bucket(bucket)
      .key(remoteKey)
      .build()
    val body = AsyncRequestBody.fromFile(localFile)
    s3Client.putObject(request, body).map(r => Right(r.eTag()))
  }

  private def asHashLookup: Stream[S3Object] => HashLookup =
    os => HashLookup(byHash(os), byKey(os))

  private def byHash(os: Stream[S3Object]) =
    os.map{o => (o.eTag, (o.key, o.lastModified))}.toMap

  private def byKey(os: Stream[S3Object]) =
    os.map{o => (o.key(), (o.eTag(), o.lastModified()))}.toMap

  def listObjects(bucket: Bucket, prefix: RemoteKey): IO[HashLookup] = {
    val request = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(prefix)
      .build()
    s3Client.listObjectsV2(request)
      .map(r => r.contents.asScala.toStream)
      .map(asHashLookup)
  }

}
