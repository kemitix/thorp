package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, MD5Hash, RemoteKey}
import net.kemitix.s3thorp.awssdk.S3Client
import org.scalatest.FunSpec

class SyncSuite extends FunSpec {

  describe("s3client thunk") {
    val testBucket = "bucket"
    val testRemoteKey = "prefix/file"
    describe("objectHead") {
      val md5Hash = "md5Hash"
      val lastModified = Instant.now()
      val sync = new Sync(new S3Client with DummyS3Client {
        override def objectHead(bucket: String, key: String) = {
          assert(bucket == testBucket)
          assert(key == testRemoteKey)
          IO(Some((md5Hash, lastModified)))
        }
      })
      it("delegates unmodified to the S3Client") {
        assertResult(Some((md5Hash, lastModified)))(
          sync.objectHead(testBucket, testRemoteKey).
            unsafeRunSync())
      }
    }
    describe("upload") {
      val md5Hash = "the-hash"
      val testLocalFile = new File("file")
      val sync = new Sync(new S3Client with DummyS3Client {
        override def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Either[Throwable, MD5Hash]] = {
          assert(localFile == testLocalFile)
          assert(bucket == testBucket)
          assert(remoteKey == testRemoteKey)
          IO(Right(md5Hash))
        }
      })
      it("delegates unmodified to the S3Client") {
        assertResult(Right(md5Hash))(
          sync.upload(testLocalFile, testBucket, testRemoteKey).
            unsafeRunSync())
      }
    }
  }
  describe("run") {

  }
}
