package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.{AmazonS3Exception, CopyObjectResult}
import net.kemitix.thorp.console.MyConsole
import net.kemitix.thorp.domain.StorageQueueEvent.{Action, ErrorQueueEvent}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.aws.S3ClientException.{
  HashMatchError,
  S3Exception
}
import org.scalatest.FreeSpec
import zio.Runtime
import zio.internal.PlatformLive

class CopierTest extends FreeSpec {

  private val runtime = Runtime(MyConsole.Live, PlatformLive.Default)

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
              .returns(_ => Some(new CopyObjectResult))
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.storageService)
            assertResult(expected)(result)
          }
        }
      }
      "when source hash does not match" - {
        "skip the file with an error" in {
          new AmazonS3ClientTestFixture {
            (fixture.amazonS3Client.copyObject _)
              .when()
              .returns(_ => None)
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.storageService)
            result match {
              case Right(
                  ErrorQueueEvent(Action.Copy("sourceKey => targetKey"),
                                  RemoteKey("targetKey"),
                                  e)) =>
                e match {
                  case HashMatchError => assert(true)
                  case _              => fail("Not a HashMatchError")
                }
              case e => fail("Not an ErrorQueueEvent: " + e)
            }
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
              case e => fail("Not an ErrorQueueEvent: " + e)
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
