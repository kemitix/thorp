package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.{AmazonS3Exception, CopyObjectResult}
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.StorageEvent.ErrorEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.aws.S3ClientException.{CopyError, HashError}
import org.scalatest.FreeSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task}

class CopierTest extends FreeSpec {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  "copier" - {
    val bucket    = Bucket.named("aBucket")
    val sourceKey = RemoteKey.create("sourceKey")
    val hash      = MD5Hash.create("aHash")
    val targetKey = RemoteKey.create("targetKey")
    "when source exists" - {
      "when source hash matches" - {
        "copies from source to target" in {
          val event    = StorageEvent.copyEvent(sourceKey, targetKey)
          val expected = Right(event)
          new AmazonS3ClientTestFixture {
            (() => fixture.amazonS3Client.copyObject)
              .when()
              .returns(_ => Task.succeed(Some(new CopyObjectResult)))
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.amazonS3Client)
            assertResult(expected)(result)
          }
        }
      }
      "when source hash does not match" - {
        "skip the file with an error" in {
          new AmazonS3ClientTestFixture {
            (() => fixture.amazonS3Client.copyObject)
              .when()
              .returns(_ => Task.succeed(None))
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.amazonS3Client)
            result match {
              case right: Right[Throwable, StorageEvent] => {
                val e = right.value.asInstanceOf[ErrorEvent].e
                e match {
                  case HashError => assert(true)
                  case _         => fail(s"Not a HashError: ${e.getMessage}")
                }
              }
              case e => fail(s"Not an ErrorQueueEvent: $e")
            }
          }
        }
      }
      "when client throws an exception" - {
        "skip the file with an error" in {
          new AmazonS3ClientTestFixture {
            private val expectedMessage = "The specified key does not exist"
            (() => fixture.amazonS3Client.copyObject)
              .when()
              .returns(_ => Task.fail(new AmazonS3Exception(expectedMessage)))
            private val result =
              invoke(bucket, sourceKey, hash, targetKey, fixture.amazonS3Client)
            val key = RemoteKey.create("targetKey")
            result match {
              case right: Right[Throwable, StorageEvent] => {
                val e = right.value.asInstanceOf[ErrorEvent].e
                e match {
                  case CopyError(cause) =>
                    assert(cause.getMessage.startsWith(expectedMessage))
                  case _ => fail(s"Not a CopyError: ${e.getMessage}")
                }
              }
              case e => fail(s"Not an ErrorQueueEvent: ${e}")
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
        amazonS3Client: AmazonS3.Client
    ) =
      runtime.unsafeRunSync {
        Copier.copy(amazonS3Client)(
          Copier.Request(bucket, sourceKey, hash, targetKey))
      }.toEither
  }

}
