package net.kemitix.s3thorp.awssdk

import java.io.File
import java.time.Instant

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp._
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, PutObjectResponse}

class S3ClientSuite
  extends UnitTest
    with KeyGenerator {

  val source = Resource(this, "../upload")

  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val fileToKey = generateKey(config.source, config.prefix) _

  describe("getS3Status") {
    val hash = MD5Hash("hash")
    val localFile = aLocalFile("the-file", hash, source, fileToKey)
    val key = localFile.remoteKey
    val keyotherkey = aLocalFile("other-key-same-hash", hash, source, fileToKey)
    val diffhash = MD5Hash("diff")
    val keydiffhash = aLocalFile("other-key-diff-hash", diffhash, source, fileToKey)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified), KeyModified(keyotherkey.remoteKey, lastModified)),
        diffhash -> Set(KeyModified(keydiffhash.remoteKey, lastModified))),
      byKey = Map(
        key -> HashModified(hash, lastModified),
        keyotherkey.remoteKey -> HashModified(hash, lastModified),
        keydiffhash.remoteKey -> HashModified(diffhash, lastModified)))

    def invoke(self: S3Client, localFile: LocalFile) = {
      self.getS3Status(localFile)(s3ObjectsData)
    }

    describe("when remote key exists") {
      val s3Client = S3Client.defaultClient
      it("should return (Some, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(hash, lastModified)),
            Set(
              KeyModified(key, lastModified),
              KeyModified(keyotherkey.remoteKey, lastModified)))
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val s3Client = S3Client.defaultClient
      it("should return (None, Set.empty)") {
        val localFile = aLocalFile("missing-file", MD5Hash("unique"), source, fileToKey)
        assertResult(
          (None,
            Set.empty)
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key exists and no others match hash") {
      val s3Client = S3Client.defaultClient
      it("should return (None, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(diffhash, lastModified)),
            Set(KeyModified(keydiffhash.remoteKey, lastModified)))
        )(invoke(s3Client, keydiffhash))
      }
    }

  }

  describe("upload") {
    def invoke(s3Client: ThorpS3Client, localFile: LocalFile, bucket: Bucket) =
      s3Client.upload(localFile, bucket, 1).unsafeRunSync
    describe("when uploading a file") {
      val md5Hash = MD5Hash("the-md5hash")
      val s3Client = new ThorpS3Client(
        new S3CatsIOClient with JavaClientWrapper {
          override def putObject(putObjectRequest: PutObjectRequest, requestBody: RB) =
            IO(PutObjectResponse.builder().eTag(md5Hash.hash).build())
        })
      val source = new File("/")
      val prefix = RemoteKey("prefix")
      val localFile: LocalFile = aLocalFile("/some/file", md5Hash, source, generateKey(source, prefix))
      val bucket: Bucket = Bucket("a-bucket")
      val remoteKey: RemoteKey = RemoteKey("prefix/some/file")
      it("should return hash of uploaded file") {
        assertResult(UploadS3Action(remoteKey, md5Hash))(invoke(s3Client, localFile, bucket))
      }
    }
  }
}
