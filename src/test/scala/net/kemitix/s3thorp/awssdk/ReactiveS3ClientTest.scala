package net.kemitix.s3thorp.awssdk

import java.time.Instant

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, HeadObjectResponse}

class ReactiveS3ClientTest extends FunSpec {

  describe("testObjectHead") {
    def invoke(self: S3Client) = {
      self.objectHead("bucket", "remoteKey").unsafeRunSync()
    }

    describe("when response is okay") {
      val expectedHash = "hash"
      val expectedLastModified = Instant.now
      new ReactiveS3Client { self: S3Client => {
          it("should return Some(expected values)") {
            val result: Option[(String, Instant)] = invoke(self)
            assertResult(Some((expectedHash, expectedLastModified)))(result)
          }
        }
        override def s3Client: S3CatsIOClient = new S3CatsIOClient with UnderlyingS3AsyncClient {
          override def headObject(headObjectRequest: HeadObjectRequest): IO[HeadObjectResponse] =
            IO(HeadObjectResponse.builder().
              eTag(expectedHash).
              lastModified(expectedLastModified).
              build())
        }
      }

    }
//    describe("when throws NoSuchKeyException") {
//      new ReactiveS3Client with S3CatsIOClientProvider { self =>
//        it("should return None") {
//          assertResult(None)(result)
//        }
//      }
//    }
  }

}
