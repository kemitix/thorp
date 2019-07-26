package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.{Bucket, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.aws.S3ObjectsByHash.byHash
import net.kemitix.thorp.storage.aws.S3ObjectsByKey.byKey
import zio.{Task, TaskR}

import scala.collection.JavaConverters._

trait Lister {

  private type Token = String
  private type Batch = (Stream[S3ObjectSummary], Option[Token])

  def listObjects(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      prefix: RemoteKey
  ): TaskR[Console, S3ObjectsData] = {

    val requestMore: Token => ListObjectsV2Request = (token: Token) =>
      new ListObjectsV2Request()
        .withBucketName(bucket.name)
        .withPrefix(prefix.key)
        .withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => TaskR[Console, Batch] =
      request =>
        for {
          _     <- ListerLogger.logFetchBatch
          batch <- tryFetchBatch(amazonS3)(request)
        } yield batch

    def fetchMore: Option[Token] => TaskR[Console, Stream[S3ObjectSummary]] = {
      case None        => TaskR.succeed(Stream.empty)
      case Some(token) => fetch(requestMore(token))
    }

    def fetch: ListObjectsV2Request => TaskR[Console, Stream[S3ObjectSummary]] =
      request => {
        for {
          batch <- fetchBatch(request)
          (summaries, more) = batch
          rest <- fetchMore(more)
        } yield summaries ++ rest
      }

    for {
      summaries <- fetch(
        new ListObjectsV2Request()
          .withBucketName(bucket.name)
          .withPrefix(prefix.key))
    } yield S3ObjectsData(byHash(summaries), byKey(summaries))
  }

  private def tryFetchBatch(amazonS3: AmazonS3.Client)
    : ListObjectsV2Request => Task[(Stream[S3ObjectSummary], Option[Token])] =
    request =>
      amazonS3
        .listObjectsV2(request)
        .map { result =>
          val more: Option[Token] =
            if (result.isTruncated) Some(result.getNextContinuationToken)
            else None
          (result.getObjectSummaries.asScala.toStream, more)
      }
}

object Lister extends Lister
