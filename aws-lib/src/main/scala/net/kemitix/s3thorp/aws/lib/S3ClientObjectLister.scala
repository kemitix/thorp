package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logListObjectsFinish, logListObjectsStart}
import net.kemitix.s3thorp.core.QuoteStripper.stripQuotes
import net.kemitix.s3thorp.domain._

import scala.collection.JavaConverters._

class S3ClientObjectLister(amazonS3: AmazonS3)
  extends S3ObjectsByHash {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey)
                 (implicit info: Int => String => Unit): IO[S3ObjectsData] = {
    val request = new ListObjectsV2Request()
        .withBucketName(bucket.name)
        .withPrefix(prefix.key)
    IO {
      amazonS3.listObjectsV2(request)
    }.bracket(
      logListObjectsStart(bucket, prefix))(
      logListObjectsFinish(bucket,prefix))
      .map(_.getObjectSummaries)
      .map(_.asScala)
      .map(_.toStream)
      .map(os => S3ObjectsData(byHash(os), byKey(os)))
  }

  private def byHash(os: Stream[S3ObjectSummary]) = {
    val mD5HashToS3Objects: Map[MD5Hash, Stream[S3ObjectSummary]] =
      os.groupBy(o => MD5Hash(o.getETag.filter{c => c != '"'}))
    val hashToModifieds: Map[MD5Hash, Set[KeyModified]] =
      mD5HashToS3Objects.mapValues { os =>
        os.map { o =>
          KeyModified(RemoteKey(o.getKey), LastModified(o.getLastModified.toInstant))}.toSet }
    hashToModifieds
  }

  private def byKey(os: Stream[S3ObjectSummary]) =
    os.map { o => {
      val remoteKey = RemoteKey(o.getKey)
      val hash = MD5Hash(o.getETag filter stripQuotes)
      val lastModified = LastModified(o.getLastModified.toInstant)
      (remoteKey, HashModified(hash, lastModified))
    }}.toMap

}
