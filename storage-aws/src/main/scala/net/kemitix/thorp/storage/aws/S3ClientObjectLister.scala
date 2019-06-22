package net.kemitix.thorp.storage.aws

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain.{Bucket, Logger, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.aws.S3ClientLogging.{logListObjectsStart, logListObjectsFinish}
import net.kemitix.thorp.storage.aws.S3ObjectsByHash.byHash
import net.kemitix.thorp.storage.aws.S3ObjectsByKey.byKey

import scala.collection.JavaConverters._

class S3ClientObjectLister(amazonS3: AmazonS3) {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey)
                 (implicit logger: Logger): IO[S3ObjectsData] = {

    type Token = String
    type Batch = (Stream[S3ObjectSummary], Option[Token])

    val requestMore = (token:Token) => new ListObjectsV2Request()
      .withBucketName(bucket.name)
      .withPrefix(prefix.key)
      .withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => IO[Batch] =
      request => IO.pure {
        val result = amazonS3.listObjectsV2(request)
        val more: Option[Token] =
          if (result.isTruncated) Some(result.getNextContinuationToken)
          else None
        (result.getObjectSummaries.asScala.toStream, more)
      }

    def fetchMore(more: Option[Token]): IO[Stream[S3ObjectSummary]] = {
      more match {
        case None => IO.pure(Stream.empty)
        case Some(token) => fetch(requestMore(token))
      }
    }

    def fetch: ListObjectsV2Request => IO[Stream[S3ObjectSummary]] =
      request =>
          for {
            batch <- fetchBatch(request)
            (summaries, more) = batch
            rest <- fetchMore(more)
          } yield summaries ++ rest

    for {
      _ <- logListObjectsStart(bucket, prefix)
      r = new ListObjectsV2Request().withBucketName(bucket.name).withPrefix(prefix.key)
      summaries <- fetch(r)
      _ <- logListObjectsFinish(bucket, prefix)
    } yield domain.S3ObjectsData(byHash(summaries), byKey(summaries))
  }

}
