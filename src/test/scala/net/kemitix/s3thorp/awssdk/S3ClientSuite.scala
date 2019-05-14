package net.kemitix.s3thorp.awssdk

import java.io.File
import java.time.Instant

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, RemoteKey}
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3.model._

class S3ClientSuite extends FunSpec {

  describe("objectHead") {
    def invoke(self: S3Client) = {
      self.objectHead("bucket", "remoteKey").unsafeRunSync()
    }

    describe("when underlying client response is okay") {
      val expectedHash = "hash"
      val expectedLastModified = Instant.now
      val s3Client = new ThorpS3Client(new S3CatsIOClient with JavaClientWrapper {
        override def headObject(headObjectRequest: HeadObjectRequest) =
          IO(HeadObjectResponse.builder().
            eTag(expectedHash).
            lastModified(expectedLastModified).
            build())
      })
      it("should return Some(expected values)") {
        assertResult(Some((expectedHash, expectedLastModified)))(invoke(s3Client))
      }
    }

    describe("when underlying client throws NoSuchKeyException") {
      val s3Client = new ThorpS3Client(new S3CatsIOClient with JavaClientWrapper {
        override def headObject(headObjectRequest: HeadObjectRequest) =
          IO(throw NoSuchKeyException.builder().build())
      })
        it("should return None") {
          assertResult(None)(invoke(s3Client))
        }
      }

  }

  describe("upload") {
    def invoke(s3Client: ThorpS3Client, localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey) =
      s3Client.upload(localFile, bucket, remoteKey).unsafeRunSync
    describe("when uploading a file") {
      val md5Hash = "the-md5hash"
      val s3Client = new ThorpS3Client(
        new S3CatsIOClient with JavaClientWrapper {
          override def putObject(putObjectRequest: PutObjectRequest, requestBody: RB) =
            IO(PutObjectResponse.builder().eTag(md5Hash).build())
        })
      val localFile: LocalFile = new File("/some/file")
      val bucket: Bucket = "a-bucket"
      val remoteKey: RemoteKey = "prefix/file"
      it("should return hash of uploaded file") {
        assertResult(Right(md5Hash))(invoke(s3Client, localFile, bucket, remoteKey))
      }
    }
  }
}
