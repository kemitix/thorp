package net.kemitix.s3thorp.awssdk

import java.time.Instant

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, HeadObjectResponse, NoSuchKeyException}

class S3ClientSuite extends FunSpec {

  describe("testObjectHead") {
    def invoke(self: S3Client) = {
      self.objectHead("bucket", "remoteKey").unsafeRunSync()
    }

    describe("when underlying client response is okay") {
      val expectedHash = "hash"
      val expectedLastModified = Instant.now
      val underlyingClient = new S3CatsIOClient with JavaClientWrapper {
        override def headObject(headObjectRequest: HeadObjectRequest) =
          IO(HeadObjectResponse.builder().
            eTag(expectedHash).
            lastModified(expectedLastModified).
            build())
      }
      val s3Client = new ThropS3Client(underlyingClient)
      it("should return Some(expected values)") {
        assertResult(Some((expectedHash, expectedLastModified)))(invoke(s3Client))
      }
    }

    describe("when underlying client throws NoSuchKeyException") {
      val underlyingClient = new S3CatsIOClient with JavaClientWrapper {
        override def headObject(headObjectRequest: HeadObjectRequest): IO[HeadObjectResponse] =
          IO(throw NoSuchKeyException.builder().build())
      }
      val s3Client = new ThropS3Client(underlyingClient)
        it("should return None") {
          assertResult(None)(invoke(s3Client))
        }
      }

    }
}
