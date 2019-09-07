package net.kemitix.thorp.storage.aws

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageEvent.{
  ActionSummary,
  DeleteEvent,
  ErrorEvent
}
import net.kemitix.thorp.domain.{Bucket, RemoteKey}
import org.scalatest.FreeSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task, UIO}

class DeleterTest extends FreeSpec {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  "delete" - {
    val bucket    = Bucket("aBucket")
    val remoteKey = RemoteKey("aRemoteKey")
    "when no errors" in {
      val expected = Right(DeleteEvent(remoteKey))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.deleteObject _)
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
          ErrorEvent(ActionSummary.Delete(remoteKey.key), remoteKey, exception))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.deleteObject _)
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
          ErrorEvent(ActionSummary.Delete(remoteKey.key), remoteKey, exception))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3Client.deleteObject _)
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
