package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logListObjectsFinish, logListObjectsStart}
import net.kemitix.s3thorp.aws.lib.S3ObjectsByHash.byHash
import net.kemitix.s3thorp.aws.lib.S3ObjectsByKey.byKey
import net.kemitix.s3thorp.domain._

import scala.collection.JavaConverters._

class S3ClientObjectLister(amazonS3: AmazonS3) {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey)
                 (implicit info: Int => String => IO[Unit]): IO[S3ObjectsData] = {

    type Token = String
    type Batch = (Stream[S3ObjectSummary], Option[Token])

    val requestMore = (token:Token) => new ListObjectsV2Request()
      .withBucketName(bucket.name)
      .withPrefix(prefix.key)
      .withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => IO[Batch] =
      request => IO {
        val result = amazonS3.listObjectsV2(request)
        val more: Option[Token] =
          if (result.isTruncated) Some(result.getNextContinuationToken)
          else None
        (result.getObjectSummaries.asScala.toStream, more)
      }

    def fetch: ListObjectsV2Request => IO[Stream[S3ObjectSummary]] =
      request =>
          for {
            batch <- fetchBatch(request)
            (summaries, more) = batch
            rest <- more match {
              case None => IO{Stream()}
              case Some(token) => fetch(requestMore(token))
            }
          } yield summaries ++ rest

    IO {
      new ListObjectsV2Request()
        .withBucketName(bucket.name)
        .withPrefix(prefix.key)
    }.bracket {
      request =>
        for {
          _ <- logListObjectsStart[IO](bucket, prefix)
          summaries <- fetch(request)
        } yield summaries
    }(_ => logListObjectsFinish[IO](bucket,prefix))
      .map(os => S3ObjectsData(byHash(os), byKey(os)))
  }

}
