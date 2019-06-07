package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ListObjectsV2Request
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logListObjectsFinish, logListObjectsStart}
import net.kemitix.s3thorp.aws.lib.S3ObjectsByHash.byHash
import net.kemitix.s3thorp.aws.lib.S3ObjectsByKey.byKey
import net.kemitix.s3thorp.domain._

import scala.collection.JavaConverters._

class S3ClientObjectLister(amazonS3: AmazonS3) {

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

}
