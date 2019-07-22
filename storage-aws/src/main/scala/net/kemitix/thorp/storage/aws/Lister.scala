package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain.{Bucket, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.aws.S3ObjectsByHash.byHash
import net.kemitix.thorp.storage.aws.S3ObjectsByKey.byKey
import zio.{IO, Task, TaskR, ZIO}

import scala.collection.JavaConverters._

class Lister(amazonS3: AmazonS3) {

  private type Token = String
  private type Batch = (Stream[S3ObjectSummary], Option[Token])

  def listObjects(
      bucket: Bucket,
      prefix: RemoteKey
  ): TaskR[MyConsole, S3ObjectsData] = {

    val requestMore = (token: Token) =>
      new ListObjectsV2Request()
        .withBucketName(bucket.name)
        .withPrefix(prefix.key)
        .withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => TaskR[MyConsole, Batch] =
      request =>
        for {
          _     <- ListerLogger.logFetchBatch
          batch <- tryFetchBatch(request)
        } yield batch

    def fetchMore(
        more: Option[Token]
    ): TaskR[MyConsole, Stream[S3ObjectSummary]] = {
      more match {
        case None        => ZIO.succeed(Stream.empty)
        case Some(token) => fetch(requestMore(token))
      }
    }

    def fetch
      : ListObjectsV2Request => TaskR[MyConsole, Stream[S3ObjectSummary]] =
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
    } yield domain.S3ObjectsData(byHash(summaries), byKey(summaries))
  }

  private def tryFetchBatch(
      request: ListObjectsV2Request
  ): Task[(Stream[S3ObjectSummary], Option[Token])] =
    IO(amazonS3.listObjectsV2(request))
      .map { result =>
        val more: Option[Token] =
          if (result.isTruncated) Some(result.getNextContinuationToken)
          else None
        (result.getObjectSummaries.asScala.toStream, more)
      }
}
