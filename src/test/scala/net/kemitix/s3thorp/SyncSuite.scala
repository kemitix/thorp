package net.kemitix.s3thorp

import java.io.File
import java.time.Instant
import java.util.concurrent.CompletableFuture

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.{S3Client, S3ObjectsData}
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.{S3AsyncClient => JavaS3AsyncClient}
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, ListObjectsV2Response, PutObjectRequest, PutObjectResponse}

class SyncSuite
  extends UnitTest
    with KeyGenerator {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val lastModified = LastModified(Instant.now)

  describe("s3client thunk") {
    val testBucket = Bucket("bucket")
    val prefix = RemoteKey("prefix")
    val source = new File("/")
    describe("upload") {
      val md5Hash = MD5Hash("the-hash")
      val testLocalFile = aLocalFile("file", md5Hash, source, generateKey(source, prefix))
      val sync = new Sync(new S3Client with DummyS3Client {
        override def upload(localFile: LocalFile, bucket: Bucket)(implicit c: Config) = IO {
          assert(bucket == testBucket)
          UploadS3Action(localFile.remoteKey, md5Hash)
        }
      })
      it("delegates unmodified to the S3Client") {
        assertResult(UploadS3Action(RemoteKey(prefix.key + "/file"), md5Hash))(
          sync.upload(testLocalFile, testBucket).
            unsafeRunSync())
      }
    }
  }
  describe("run") {
    val testBucket = Bucket("bucket")
    val source = Resource(this, "upload")
    // source contains the files root-file and subdir/leaf-file
    val config = Config(Bucket("bucket"), RemoteKey("prefix"), source = source)
    val rootRemoteKey = RemoteKey("prefix/root-file")
    val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")
    val rootHash = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
    val leafHash = MD5Hash("208386a650bdec61cfcd7bd8dcb6b542")
    describe("when all files should be uploaded") {
      val sync = new RecordingSync(testBucket, new DummyS3Client {}, S3ObjectsData(
        byHash = Map(),
        byKey = Map()))
      sync.run(config).unsafeRunSync
      it("uploads all files") {
        val expectedUploads = Map(
          "subdir/leaf-file" -> leafRemoteKey,
          "root-file" -> rootRemoteKey
        )
        assertResult(expectedUploads)(sync.uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        assertResult(expectedCopies)(sync.copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        assertResult(expectedDeletions)(sync.deletionsRecord)
      }
    }
    describe("when no files should be uploaded") {
      val s3ObjectsData = S3ObjectsData(
        byHash = Map(
          rootHash -> Set(KeyModified(RemoteKey("prefix/root-file"), lastModified)),
          leafHash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
        byKey = Map(
          RemoteKey("prefix/root-file") -> HashModified(rootHash, lastModified),
          RemoteKey("prefix/subdir/leaf-file") -> HashModified(leafHash, lastModified)))
      val sync = new RecordingSync(testBucket, new DummyS3Client {}, s3ObjectsData)
      sync.run(config).unsafeRunSync
      it("uploads nothing") {
        val expectedUploads = Map()
        assertResult(expectedUploads)(sync.uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        assertResult(expectedCopies)(sync.copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        assertResult(expectedDeletions)(sync.deletionsRecord)
      }
    }
    describe("when a file is renamed it is moved on S3 with no upload") {
      // 'root-file-old' should be renamed as 'root-file'
      val rootHash = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
      val leafHash = MD5Hash("208386a650bdec61cfcd7bd8dcb6b542")
      val s3ObjectsData = S3ObjectsData(
        byHash = Map(
          rootHash -> Set(KeyModified(RemoteKey("prefix/root-file-old"), lastModified)),
          leafHash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
        byKey = Map(
          RemoteKey("prefix/root-file-old") -> HashModified(rootHash, lastModified),
          RemoteKey("prefix/subdir/leaf-file") -> HashModified(leafHash, lastModified)))
      val sync = new RecordingSync(testBucket, new DummyS3Client {}, s3ObjectsData)
      sync.run(config).unsafeRunSync
      it("uploads nothing") {
        val expectedUploads = Map()
        assertResult(expectedUploads)(sync.uploadsRecord)
      }
      it("copies the file") {
        val expectedCopies = Map(RemoteKey("prefix/root-file-old") -> RemoteKey("prefix/root-file"))
        assertResult(expectedCopies)(sync.copiesRecord)
      }
      it("deletes the original") {
        val expectedDeletions = Set(RemoteKey("prefix/root-file-old"))
        assertResult(expectedDeletions)(sync.deletionsRecord)
      }
    }
    describe("when a file is copied it is copied on S3 with no upload") {
      it("TODO") {
        pending
      }
    }
    describe("when a file is deleted locally it is deleted from S3") {
      val deletedHash = MD5Hash("deleted-hash")
      val deletedKey = RemoteKey("prefix/deleted-file")
      val s3ObjectsData = S3ObjectsData(
        byHash = Map(
          deletedHash -> Set(KeyModified(RemoteKey("prefix/deleted-file"), lastModified))),
        byKey = Map(
          deletedKey -> HashModified(deletedHash, lastModified)))
      val sync = new RecordingSync(testBucket, new DummyS3Client {}, s3ObjectsData)
      sync.run(config).unsafeRunSync
      it("deleted key") {
        val expectedDeletions = Set(deletedKey)
        assertResult(expectedDeletions)(sync.deletionsRecord)
      }
    }
    describe("io actions execute") {
      val recordingS3Client = new RecordingS3Client
      val client = S3Client.createClient(recordingS3Client)
      val sync = new Sync(client)
      sync.run(config).unsafeRunSync
      it("invokes the underlying Java s3client") {
        val expected = Set(
          PutObjectRequest.builder().bucket(testBucket.name).key(rootRemoteKey.key).build(),
          PutObjectRequest.builder().bucket(testBucket.name).key(leafRemoteKey.key).build()
        )
        val result = recordingS3Client.puts
        assertResult(expected)(result)
      }
    }
    describe("when a file is file is excluded") {
      val filteredConfig = config.copy(filters = Set(Filter("leaf")))
      val sync = new RecordingSync(testBucket, new DummyS3Client {}, S3ObjectsData(Map(), Map()))
      sync.run(filteredConfig).unsafeRunSync
      it("is not uploaded") {
        val expectedUploads = Map(
          "root-file" -> rootRemoteKey
        )
        assertResult(expectedUploads)(sync.uploadsRecord)
      }
    }
  }

  class RecordingSync(testBucket: Bucket, s3Client: S3Client, s3ObjectsData: S3ObjectsData)
    extends Sync(s3Client) {

    var uploadsRecord: Map[String, RemoteKey] = Map()
    var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
    var deletionsRecord: Set[RemoteKey] = Set()

    override def listObjects(bucket: Bucket, prefix: RemoteKey)(implicit c: Config) = IO {s3ObjectsData}

    override def upload(localFile: LocalFile,
                        bucket: Bucket
                       )(implicit c: Config) = IO {
      if (bucket == testBucket)
        uploadsRecord += (localFile.relative.toString -> localFile.remoteKey)
      UploadS3Action(localFile.remoteKey, MD5Hash("some hash value"))
    }

    override def copy(bucket: Bucket,
                      sourceKey: RemoteKey,
                      hash: MD5Hash,
                      targetKey: RemoteKey
                     )(implicit c: Config) = IO {
      if (bucket == testBucket)
        copiesRecord += (sourceKey -> targetKey)
      CopyS3Action(targetKey)
    }

    override def delete(bucket: Bucket,
                        remoteKey: RemoteKey
                       )(implicit c: Config) = IO {
      if (bucket == testBucket)
        deletionsRecord += remoteKey
      DeleteS3Action(remoteKey)
    }
  }

  class RecordingS3Client extends S3AsyncClient {
    var lists: Set[ListObjectsV2Request] = Set()
    var puts: Set[PutObjectRequest] = Set()
    override val underlying: s3.S3AsyncClient = new JavaS3AsyncClient {
      override def serviceName(): String = "s3Recorder"

      override def close(): Unit = ()

      override def listObjectsV2(listObjectsV2Request: ListObjectsV2Request): CompletableFuture[ListObjectsV2Response] = {
        lists += listObjectsV2Request
        CompletableFuture.completedFuture(ListObjectsV2Response.builder().build())
      }

      override def putObject(putObjectRequest: PutObjectRequest,
                             requestBody: AsyncRequestBody): CompletableFuture[PutObjectResponse] = {
        puts += putObjectRequest
        CompletableFuture.completedFuture(PutObjectResponse.builder().eTag("not-null").build())
      }

    }
  }
}
