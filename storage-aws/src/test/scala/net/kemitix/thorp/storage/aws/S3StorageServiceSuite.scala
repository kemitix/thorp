package net.kemitix.thorp.storage.aws

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

import com.amazonaws.services.s3.model.{
  AmazonS3Exception,
  CopyObjectResult,
  ListObjectsV2Result,
  S3ObjectSummary
}
import net.kemitix.thorp.console.MyConsole
import net.kemitix.thorp.core.Resource
import net.kemitix.thorp.domain.StorageQueueEvent.{
  Action,
  DoNothingQueueEvent,
  ErrorQueueEvent
}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.aws.S3ClientException.S3Exception
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import zio.Runtime
import zio.internal.PlatformLive

class S3StorageServiceSuite extends FreeSpec with MockFactory {

  private val runtime = Runtime(MyConsole.Live, PlatformLive.Default)

  "listObjects" - {
    def objectSummary(hash: MD5Hash,
                      remoteKey: RemoteKey,
                      lastModified: LastModified) = {
      val summary = new S3ObjectSummary()
      summary.setETag(hash.hash)
      summary.setKey(remoteKey.key)
      summary.setLastModified(Date.from(lastModified.when))
      summary
    }
    val source     = Resource(this, "upload")
    val sourcePath = source.toPath
    val prefix     = RemoteKey("prefix")
    implicit val config: Config =
      Config(Bucket("bucket"), prefix, sources = Sources(List(sourcePath)))
    val lm             = LastModified(Instant.now.truncatedTo(ChronoUnit.MILLIS))
    val h1             = MD5Hash("hash1")
    val k1a            = RemoteKey("key1a")
    val o1a            = objectSummary(h1, k1a, lm)
    val k1b            = RemoteKey("key1b")
    val o1b            = objectSummary(h1, k1b, lm)
    val h2             = MD5Hash("hash2")
    val k2             = RemoteKey("key2")
    val o2             = objectSummary(h2, k2, lm)
    val myFakeResponse = new ListObjectsV2Result()
    val summaries      = myFakeResponse.getObjectSummaries
    summaries.add(o1a)
    summaries.add(o1b)
    summaries.add(o2)

    "should build list of hash lookups, with duplicate objects grouped by hash" in {
      val expected = Right(
        S3ObjectsData(
          byHash = Map(h1 -> Set(KeyModified(k1a, lm), KeyModified(k1b, lm)),
                       h2 -> Set(KeyModified(k2, lm))),
          byKey = Map(k1a -> HashModified(h1, lm),
                      k1b -> HashModified(h1, lm),
                      k2  -> HashModified(h2, lm))
        ))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.listObjectsV2 _)
          .when()
          .returns(_ => myFakeResponse)
        private val result = invoke(fixture.storageService)
        assertResult(expected)(result)
      }
    }
    def invoke(storageService: S3StorageService) =
      runtime.unsafeRunSync {
        storageService
          .listObjects(Bucket("bucket"), RemoteKey("prefix"))
      }.toEither
  }

  "copier" - {
    val bucket    = Bucket("aBucket")
    val sourceKey = RemoteKey("sourceKey")
    val hash      = MD5Hash("aHash")
    val targetKey = RemoteKey("targetKey")
    "when source exists" - {
      "when source hash matches" - {
        "copies from source to target" in {
          val event    = StorageQueueEvent.CopyQueueEvent(sourceKey, targetKey)
          val expected = Right(event)
          new AmazonS3ClientTestFixture {
            (fixture.amazonS3Client.copyObject _)
              .when()
              .returns(_ => new CopyObjectResult)
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.storageService)
            assertResult(expected)(result)
          }
        }
      }
      "when source hash does not match" - {
        "skip the file with an error" in {
          val event    = DoNothingQueueEvent(targetKey)
          val expected = Right(event)
          new AmazonS3ClientTestFixture {
            (fixture.amazonS3Client.copyObject _)
              .when()
              .returns(_ => null)
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.storageService)
            assertResult(expected)(result)
          }
        }
      }
      "when client throws an exception" - {
        "skip the file with an error" in {
          new AmazonS3ClientTestFixture {
            private val expectedMessage = "The specified key does not exist"
            (fixture.amazonS3Client.copyObject _)
              .when()
              .throws(new AmazonS3Exception(expectedMessage))
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.storageService)
            result match {
              case Right(
                  ErrorQueueEvent(Action.Copy("sourceKey => targetKey"),
                                  RemoteKey("targetKey"),
                                  e)) =>
                e match {
                  case S3Exception(message) =>
                    assert(message.startsWith(expectedMessage))
                  case _ => fail("Not an S3Exception")
                }
              case e => fail("Not an ErrorQueueEvent: " + e.toString)
            }
          }
        }
      }
    }
    def invoke(
        bucket: Bucket,
        sourceKey: RemoteKey,
        hash: MD5Hash,
        targetKey: RemoteKey,
        storageService: S3StorageService
    ) =
      runtime.unsafeRunSync {
        storageService.copy(bucket, sourceKey, hash, targetKey)
      }.toEither
  }

}
