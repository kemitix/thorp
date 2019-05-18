package net.kemitix.s3thorp.awssdk

import java.io.File
import java.time.Instant

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, HashModified, LastModified, MD5Hash, RemoteKey, UploadS3Action}
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, PutObjectResponse}

class S3ClientSuite extends FunSpec {

  describe("objectHead") {
    val key = RemoteKey("key")
    val hash = MD5Hash("hash")
    val lastModified = LastModified(Instant.now)
    val hashLookup: HashLookup = HashLookup(
      byHash = Map(hash -> (key, lastModified)),
      byKey = Map(key -> HashModified(hash, lastModified)))

    def invoke(self: S3Client, remoteKey: RemoteKey) = {
      self.objectHead(remoteKey)(hashLookup)
    }

    describe("when remote key exists") {
      val s3Client = S3Client.defaultClient
      it("should return Some(expected values)") {
        assertResult(Some(HashModified(hash, lastModified)))(invoke(s3Client, key))
      }
    }

    describe("when remote key does not exist") {
      val s3Client = S3Client.defaultClient
      it("should return None") {
        assertResult(None)(invoke(s3Client, RemoteKey("missing-key")))
      }
    }

  }

  describe("upload") {
    def invoke(s3Client: ThorpS3Client, localFile: File, bucket: Bucket, remoteKey: RemoteKey) =
      s3Client.upload(localFile, bucket, remoteKey).unsafeRunSync
    describe("when uploading a file") {
      val md5Hash = MD5Hash("the-md5hash")
      val s3Client = new ThorpS3Client(
        new S3CatsIOClient with JavaClientWrapper {
          override def putObject(putObjectRequest: PutObjectRequest, requestBody: RB) =
            IO(PutObjectResponse.builder().eTag(md5Hash.hash).build())
        })
      val localFile: File = new File("/some/file")
      val bucket: Bucket = Bucket("a-bucket")
      val remoteKey: RemoteKey = RemoteKey("prefix/file")
      it("should return hash of uploaded file") {
        assertResult(UploadS3Action(remoteKey, md5Hash))(invoke(s3Client, localFile, bucket, remoteKey))
      }
    }
  }
}
