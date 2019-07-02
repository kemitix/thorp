package net.kemitix.thorp.storage.aws

import cats.data.EitherT
import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain.{Bucket, Logger, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.aws.S3ObjectsByHash.byHash
import net.kemitix.thorp.storage.aws.S3ObjectsByKey.byKey

import scala.collection.JavaConverters._
import scala.util.Try

class Lister(amazonS3: AmazonS3) {

  private type Token = String
  private type Batch = (Stream[S3ObjectSummary], Option[Token])

  def listObjects(bucket: Bucket,
                  prefix: RemoteKey)
                 (implicit l: Logger): EitherT[IO, String, S3ObjectsData] = {

    val requestMore = (token:Token) => new ListObjectsV2Request()
      .withBucketName(bucket.name)
      .withPrefix(prefix.key)
      .withContinuationToken(token)

    def fetchBatch: ListObjectsV2Request => EitherT[IO, String, Batch] =
      request =>
        EitherT {
          for {
            _ <- l.info("Fetching remote summaries...")
            batch <- tryFetchBatch(request)
          } yield batch
        }

    def fetchMore(more: Option[Token]): EitherT[IO, String, Stream[S3ObjectSummary]] = {
      more match {
        case None => EitherT.right(IO.pure(Stream.empty))
        case Some(token) => fetch(requestMore(token))
      }
    }

    def fetch: ListObjectsV2Request => EitherT[IO, String, Stream[S3ObjectSummary]] =
      request => {
        for {
          batch <- fetchBatch(request)
          (summaries, more) = batch
          rest <- fetchMore(more)
        } yield summaries ++ rest
      }

    for {
      summaries <- fetch(new ListObjectsV2Request().withBucketName(bucket.name).withPrefix(prefix.key))
    } yield domain.S3ObjectsData(byHash(summaries), byKey(summaries))
  }

  private def tryFetchBatch(request: ListObjectsV2Request): IO[Either[String, (Stream[S3ObjectSummary], Option[Token])]] = {
    IO {
      Try(amazonS3.listObjectsV2(request))
        .map { result =>
          val more: Option[Token] =
            if (result.isTruncated) Some(result.getNextContinuationToken)
            else None
          (result.getObjectSummaries.asScala.toStream, more)
        }.toEither.swap.map(e => e.getMessage).swap
    }
  }
}
