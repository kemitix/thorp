package net.kemitix.thorp.storage.aws

import java.util.Date

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{
  AmazonS3Exception,
  ListObjectsV2Result,
  S3ObjectSummary
}
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain._
import org.scalatest.FreeSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task, UIO}

class ListerTest extends FreeSpec {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  "list" - {
    val bucket = Bucket("aBucket")
    val prefix = RemoteKey("aRemoteKey")
    "when no errors" - {
      "when single fetch required" in {
        val nowDate    = new Date
        val nowInstant = nowDate.toInstant
        val key        = "key"
        val etag       = "etag"
        val expectedHashMap = Map(
          MD5Hash(etag) -> Set(
            KeyModified(RemoteKey(key), LastModified(nowInstant))))
        val expectedKeyMap = Map(
          RemoteKey(key) -> HashModified(MD5Hash(etag),
                                         LastModified(nowInstant))
        )
        val expected = Right(S3ObjectsData(expectedHashMap, expectedKeyMap))
        new AmazonS3ClientTestFixture {
          (fixture.amazonS3Client.listObjectsV2 _)
            .when()
            .returns(_ => {
              UIO.succeed(objectResults(nowDate, key, etag, false))
            })
          private val result = invoke(fixture.storageService)(bucket, prefix)
          assertResult(expected)(result)
        }
      }

      "when second fetch required" in {
        val nowDate    = new Date
        val nowInstant = nowDate.toInstant
        val key1       = "key1"
        val etag1      = "etag1"
        val key2       = "key2"
        val etag2      = "etag2"
        val expectedHashMap = Map(
          MD5Hash(etag1) -> Set(
            KeyModified(RemoteKey(key1), LastModified(nowInstant))),
          MD5Hash(etag2) -> Set(
            KeyModified(RemoteKey(key2), LastModified(nowInstant)))
        )
        val expectedKeyMap = Map(
          RemoteKey(key1) -> HashModified(MD5Hash(etag1),
                                          LastModified(nowInstant)),
          RemoteKey(key2) -> HashModified(MD5Hash(etag2),
                                          LastModified(nowInstant))
        )
        val expected = Right(S3ObjectsData(expectedHashMap, expectedKeyMap))
        new AmazonS3ClientTestFixture {
          (fixture.amazonS3Client.listObjectsV2 _)
            .when()
            .returns(_ => UIO(objectResults(nowDate, key1, etag1, true)))
            .noMoreThanOnce()
          (fixture.amazonS3Client.listObjectsV2 _)
            .when()
            .returns(_ => UIO(objectResults(nowDate, key2, etag2, false)))
          private val result = invoke(fixture.storageService)(bucket, prefix)
          assertResult(expected)(result)
        }
      }

      def objectSummary(key: String, etag: String, lastModified: Date) = {
        val objectSummary = new S3ObjectSummary
        objectSummary.setKey(key)
        objectSummary.setETag(etag)
        objectSummary.setLastModified(lastModified)
        objectSummary
      }

      def objectResults(nowDate: Date,
                        key: String,
                        etag: String,
                        truncated: Boolean) = {
        val result = new ListObjectsV2Result
        result.getObjectSummaries.add(objectSummary(key, etag, nowDate))
        result.setTruncated(truncated)
        result
      }

    }
    "when Amazon Service Exception" in {
      val exception = new AmazonS3Exception("message")
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.listObjectsV2 _)
          .when()
          .returns(_ => Task.fail(exception))
        private val result = invoke(fixture.storageService)(bucket, prefix)
        assert(result.isLeft)
      }
    }
    "when Amazon SDK Client Exception" in {
      val exception = new SdkClientException("message")
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.listObjectsV2 _)
          .when()
          .returns(_ => Task.fail(exception))
        private val result = invoke(fixture.storageService)(bucket, prefix)
        assert(result.isLeft)
      }
    }
    def invoke(storageService: S3Storage)(bucket: Bucket, prefix: RemoteKey) =
      runtime.unsafeRunSync {
        storageService.listObjects(bucket, prefix)
      }.toEither

  }

}
