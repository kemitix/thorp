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
  def aLocalFileWithHash(path: String, myHash: MD5Hash): LocalFile =
    new LocalFile(source.toPath.resolve(path).toFile, source, fileToKey) {
      override def hash: MD5Hash = myHash
    }

  describe("getS3Status") {
    val hash = MD5Hash("hash")
    val localFile = aLocalFileWithHash("the-file", hash)
    val key = localFile.remoteKey
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(hash -> Set(KeyModified(key, lastModified))),
      byKey = Map(key -> HashModified(hash, lastModified)))

    def invoke(self: S3Client, localFile: LocalFile) = {
      self.getS3Status(localFile)(s3ObjectsData)
    }

    describe("when remote key exists") {
      val s3Client = S3Client.defaultClient
      it("should return Some(expected values)") {
        assertResult(
          (Some(HashModified(hash, lastModified)),
          Set(KeyModified(key, lastModified)))
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key does not exist") {
      val s3Client = S3Client.defaultClient
      it("should return None") {
        pending
        assertResult(None)(invoke(s3Client, localFile))
      }
    }

  }

  describe("upload") {
    def invoke(s3Client: ThorpS3Client, localFile: LocalFile, bucket: Bucket) =
      s3Client.upload(localFile, bucket).unsafeRunSync
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
