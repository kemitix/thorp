package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.{
  ListObjectsV2Request,
  ListObjectsV2Result,
  S3ObjectSummary
}
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.{Bucket, RemoteKey, RemoteObjects}
import net.kemitix.thorp.storage.aws.S3ObjectsByHash.byHash
import net.kemitix.thorp.storage.aws.S3ObjectsByKey.byKey
import zio.{Task, RIO}

import scala.jdk.CollectionConverters._

trait Lister {

  private type Token = String
  case class Batch(summaries: LazyList[S3ObjectSummary], more: Option[Token])

  def listObjects(amazonS3: AmazonS3.Client)(
      bucket: Bucket,
      prefix: RemoteKey
  ): RIO[Console, RemoteObjects] = {

    def request =
      new ListObjectsV2Request()
        .withBucketName(bucket.name)
        .withPrefix(prefix.key)

    def requestMore: Token => ListObjectsV2Request =
      token => request.withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => RIO[Console, Batch] =
      request => ListerLogger.logFetchBatch *> tryFetchBatch(amazonS3)(request)

    def fetchMore: Option[Token] => RIO[Console, LazyList[S3ObjectSummary]] = {
      case None        => RIO.succeed(LazyList.empty)
      case Some(token) => fetch(requestMore(token))
    }

    def fetch: ListObjectsV2Request => RIO[Console, LazyList[S3ObjectSummary]] =
      request =>
        for {
          batch <- fetchBatch(request)
          more  <- fetchMore(batch.more)
        } yield batch.summaries ++ more

    fetch(request)
      .map(summaries => {
        RemoteObjects.create(byHash(summaries), byKey(summaries))
      })
  }

  private def tryFetchBatch(
      amazonS3: AmazonS3.Client): ListObjectsV2Request => Task[Batch] =
    request =>
      amazonS3
        .listObjectsV2(request)
        .map(result => Batch(objectSummaries(result), moreToken(result)))

  private def objectSummaries(
      result: ListObjectsV2Result): LazyList[S3ObjectSummary] =
    LazyList.from(result.getObjectSummaries.asScala)

  private def moreToken(result: ListObjectsV2Result): Option[String] =
    if (result.isTruncated) Some(result.getNextContinuationToken)
    else None

}

object Lister extends Lister
