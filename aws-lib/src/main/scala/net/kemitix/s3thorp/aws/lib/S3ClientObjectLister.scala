package net.kemitix.s3thorp.aws.lib

import cats.Monad
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.s3thorp.aws.lib.S3ClientLogging.{logListObjectsFinish, logListObjectsStart}
import net.kemitix.s3thorp.aws.lib.S3ObjectsByHash.byHash
import net.kemitix.s3thorp.aws.lib.S3ObjectsByKey.byKey
import net.kemitix.s3thorp.domain._

import scala.collection.JavaConverters._

class S3ClientObjectLister[M[_]: Monad](amazonS3: AmazonS3) {

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey)
                 (implicit info: Int => String => M[Unit]): M[S3ObjectsData] = {

    type Token = String
    type Batch = (Stream[S3ObjectSummary], Option[Token])

    val requestMore = (token:Token) => new ListObjectsV2Request()
      .withBucketName(bucket.name)
      .withPrefix(prefix.key)
      .withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => M[Batch] =
      request => Monad[M].pure {
        val result = amazonS3.listObjectsV2(request)
        val more: Option[Token] =
          if (result.isTruncated) Some(result.getNextContinuationToken)
          else None
        (result.getObjectSummaries.asScala.toStream, more)
      }

    def fetchMore(more: Option[Token]): M[Stream[S3ObjectSummary]] = {
      more match {
        case None => Monad[M].pure(Stream.empty)
        case Some(token) => fetch(requestMore(token))
      }
    }

    def fetch: ListObjectsV2Request => M[Stream[S3ObjectSummary]] =
      request =>
          for {
            batch <- fetchBatch(request)
            (summaries, more) = batch
            rest <- fetchMore(more)
          } yield summaries ++ rest

    for {
      _ <- logListObjectsStart[M](bucket, prefix)
      r = new ListObjectsV2Request().withBucketName(bucket.name).withPrefix(prefix.key)
      summaries <- fetch(r)
      _ <- logListObjectsFinish[M](bucket, prefix)
    } yield S3ObjectsData(byHash(summaries), byKey(summaries))
  }

}
