package net.kemitix.thorp.storage.aws

import java.util.Date

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{
  AmazonS3Exception,
  ListObjectsV2Result,
  S3ObjectSummary
}
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.Storage
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, RIO, Task, UIO}

class ListerTest extends FreeSpec {

  "list" - {
    val bucket = Bucket.named("aBucket")
    val prefix = RemoteKey("aRemoteKey")
    "when no errors" - {
      "when single fetch required" in {
        val nowDate         = new Date
        val key             = "key"
        val etag            = "etag"
        val expectedHashMap = Map(MD5Hash.create(etag) -> RemoteKey(key))
        val expectedKeyMap  = Map(RemoteKey(key) -> MD5Hash.create(etag))
        new AmazonS3ClientTestFixture {
          (fixture.amazonS3Client.listObjectsV2 _)
            .when()
            .returns(_ => {
              UIO.succeed(objectResults(nowDate, key, etag, truncated = false))
            })
          private val result  = invoke(fixture.amazonS3Client)(bucket, prefix)
          private val hashMap = result.map(_.byHash).map(m => Map.from(m))
          private val keyMap  = result.map(_.byKey).map(m => Map.from(m))
          hashMap should be(Right(expectedHashMap))
          keyMap should be(Right(expectedKeyMap))
        }
      }

      "when second fetch required" in {
        val nowDate = new Date
        val key1    = "key1"
        val etag1   = "etag1"
        val key2    = "key2"
        val etag2   = "etag2"
        val expectedHashMap = Map(
          MD5Hash.create(etag1) -> RemoteKey(key1),
          MD5Hash.create(etag2) -> RemoteKey(key2)
        )
        val expectedKeyMap = Map(
          RemoteKey(key1) -> MD5Hash.create(etag1),
          RemoteKey(key2) -> MD5Hash.create(etag2)
        )
        new AmazonS3ClientTestFixture {

          (fixture.amazonS3Client.listObjectsV2 _)
            .when()
            .returns(_ =>
              UIO(objectResults(nowDate, key1, etag1, truncated = true)))
            .noMoreThanOnce()

          (fixture.amazonS3Client.listObjectsV2 _)
            .when()
            .returns(_ =>
              UIO(objectResults(nowDate, key2, etag2, truncated = false)))
          private val result  = invoke(fixture.amazonS3Client)(bucket, prefix)
          private val hashMap = result.map(_.byHash).map(m => Map.from(m))
          private val keyMap  = result.map(_.byKey).map(m => Map.from(m))
          hashMap should be(Right(expectedHashMap))
          keyMap should be(Right(expectedKeyMap))
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
        private val result = invoke(fixture.amazonS3Client)(bucket, prefix)
        assert(result.isLeft)
      }
    }
    "when Amazon SDK Client Exception" in {
      val exception = new SdkClientException("message")
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.listObjectsV2 _)
          .when()
          .returns(_ => Task.fail(exception))
        private val result = invoke(fixture.amazonS3Client)(bucket, prefix)
        assert(result.isLeft)
      }
    }
    def invoke(amazonS3Client: AmazonS3.Client)(bucket: Bucket,
                                                prefix: RemoteKey) = {
      object TestEnv extends Storage.Test with Console.Test
      val program: RIO[Storage with Console, RemoteObjects] = Lister
        .listObjects(amazonS3Client)(bucket, prefix)
      val runtime = new DefaultRuntime {}
      runtime.unsafeRunSync(program.provide(TestEnv)).toEither
    }

  }

}
