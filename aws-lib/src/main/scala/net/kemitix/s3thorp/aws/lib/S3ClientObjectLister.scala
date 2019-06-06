package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.domain._
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, S3Object}

import scala.collection.JavaConverters._

class S3ClientObjectLister(s3Client: S3CatsIOClient)
  extends S3ClientLogging
    with S3ObjectsByHash
    with QuoteStripper {

  def listObjects(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => Unit): IO[S3ObjectsData] = {
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

}
