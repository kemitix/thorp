package net.kemitix.thorp.storage.aws

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageEvent.ActionSummary
import net.kemitix.thorp.domain.{Bucket, RemoteKey, StorageEvent}
import org.scalatest.FreeSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task, UIO}

class DeleterTest extends FreeSpec {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  "delete" - {
    val bucket    = Bucket.named("aBucket")
    val remoteKey = RemoteKey.create("aRemoteKey")
    "when no errors" in {
      val expected = Right(StorageEvent.deleteEvent(remoteKey))
      new AmazonS3ClientTestFixture {
        (() => fixture.amazonS3Client.deleteObject)
          .when()
          .returns(_ => UIO.succeed(()))
        private val result = invoke(fixture.amazonS3Client)(bucket, remoteKey)
        assertResult(expected)(result)
      }
    }
    "when Amazon Service Exception" in {
      val exception = new AmazonS3Exception("message")
      val expected =
        Right(
          StorageEvent.errorEvent(ActionSummary.delete(remoteKey.key),
                                  remoteKey,
                                  exception))
      new AmazonS3ClientTestFixture {
        (() => fixture.amazonS3Client.deleteObject)
          .when()
          .returns(_ => Task.fail(exception))
        private val result = invoke(fixture.amazonS3Client)(bucket, remoteKey)
        assertResult(expected)(result)
      }
    }
    "when Amazon SDK Client Exception" in {
      val exception = new SdkClientException("message")
      val expected =
        Right(
          StorageEvent.errorEvent(ActionSummary.delete(remoteKey.key),
                                  remoteKey,
                                  exception))
      new AmazonS3ClientTestFixture {
        (() => fixture.amazonS3Client.deleteObject)
          .when()
          .returns(_ => Task.fail(exception))
        private val result = invoke(fixture.amazonS3Client)(bucket, remoteKey)
        assertResult(expected)(result)
      }
    }
    def invoke(amazonS3Client: AmazonS3.Client)(bucket: Bucket,
                                                remoteKey: RemoteKey) =
      runtime.unsafeRunSync {
        Deleter.delete(amazonS3Client)(bucket, remoteKey)
      }.toEither

  }
}
