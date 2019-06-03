package net.kemitix.s3thorp

import java.io.{File, InputStream}
import java.net.URL
import java.time.Instant
import java.util
import java.util.Date
import java.util.concurrent.CompletableFuture

import cats.effect.IO
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.waiters.AmazonS3Waiters
import com.amazonaws.services.s3.{AmazonS3, S3ClientOptions, S3ResponseMetadata, model}
import com.amazonaws.{AmazonWebServiceRequest, HttpMethod}
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import net.kemitix.s3thorp.awssdk.{MyAmazonS3, S3Client, S3ObjectsData, UploadProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, MD5Hash, RemoteKey}
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, ListObjectsV2Response}
import software.amazon.awssdk.services.s3.{S3AsyncClient => JavaS3AsyncClient}

class SyncSuite
  extends UnitTest
    with KeyGenerator {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val lastModified = LastModified(Instant.now)
  val fileToKey: File => RemoteKey = generateKey(source, prefix)
  val fileToHash = (file: File) => new MD5HashGenerator {}.md5File(file)
  val rootHash = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
  val leafHash = MD5Hash("208386a650bdec61cfcd7bd8dcb6b542")
  val rootFile = aLocalFile("root-file", rootHash, source, fileToKey, fileToHash)
  val leafFile = aLocalFile("subdir/leaf-file", leafHash, source, fileToKey, fileToHash)

  val md5HashGenerator: File => MD5Hash = file => new MD5HashGenerator {}.md5File(file)(config)

  describe("s3client thunk") {
    val testBucket = Bucket("bucket")
    val prefix = RemoteKey("prefix")
    val source = new File("/")
    describe("upload") {
      val md5Hash = MD5Hash("the-hash")
      val testLocalFile = aLocalFile("file", md5Hash, source, generateKey(source, prefix), fileToHash)
      val progressListener = new UploadProgressListener(testLocalFile)
      val sync = new Sync(new S3Client with DummyS3Client {
        override def upload(localFile: LocalFile,
                            bucket: Bucket,
                            progressListener: UploadProgressListener,
                            tryCount: Int)
                           (implicit c: Config) = IO {
          assert(bucket == testBucket)
          UploadS3Action(localFile.remoteKey, md5Hash)
        }
      }, md5HashGenerator)
      it("delegates unmodified to the S3Client") {
        assertResult(UploadS3Action(RemoteKey(prefix.key + "/file"), md5Hash))(
          sync.upload(testLocalFile, testBucket, progressListener, 1).
            unsafeRunSync())
      }
    }
  }

  def putObjectRequest(bucket: Bucket, remoteKey: RemoteKey, localFile: LocalFile) = {
    (bucket.name, remoteKey.key, localFile.file)
  }

  describe("run") {
    val testBucket = Bucket("bucket")
    val source = Resource(this, "upload")
    // source contains the files root-file and subdir/leaf-file
    val config = Config(Bucket("bucket"), RemoteKey("prefix"), source = source)
    val rootRemoteKey = RemoteKey("prefix/root-file")
    val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")
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
      val recordingS3ClientLegacy = new RecordingS3ClientLegacy
      val recordingS3Client = new RecordingS3Client
      val transferManager = TransferManagerBuilder.standard
        .withS3Client(recordingS3Client).build
      val client = S3Client.createClient(recordingS3ClientLegacy, recordingS3Client, transferManager)
      val sync = new Sync(client, md5HashGenerator)
      sync.run(config).unsafeRunSync
      it("invokes the underlying Java s3client") {
        val expected = Set(
          putObjectRequest(testBucket, rootRemoteKey, rootFile),
          putObjectRequest(testBucket, leafRemoteKey, leafFile)
        )
        val result = recordingS3Client.puts map {r => (r.getBucketName, r.getKey, r.getFile)}
        assertResult(expected)(result)
      }
    }
    describe("when a file is file is excluded") {
      val configWithExclusion = config.copy(excludes = List(Exclude("leaf")))
      val sync = new RecordingSync(testBucket, new DummyS3Client {}, S3ObjectsData(Map(), Map()))
      sync.run(configWithExclusion).unsafeRunSync
      it("is not uploaded") {
        val expectedUploads = Map(
          "root-file" -> rootRemoteKey
        )
        assertResult(expectedUploads)(sync.uploadsRecord)
      }
    }
  }

  class RecordingSync(testBucket: Bucket, s3Client: S3Client, s3ObjectsData: S3ObjectsData)
    extends Sync(s3Client, md5HashGenerator) {

    var uploadsRecord: Map[String, RemoteKey] = Map()
    var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
    var deletionsRecord: Set[RemoteKey] = Set()

    override def listObjects(bucket: Bucket, prefix: RemoteKey)(implicit c: Config) = IO {s3ObjectsData}

    override def upload(localFile: LocalFile,
                        bucket: Bucket,
                        progressListener: UploadProgressListener,
                        tryCount: Int
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

  class RecordingS3ClientLegacy extends S3AsyncClient {
    var lists: Set[ListObjectsV2Request] = Set()
    var puts: Set[PutObjectRequest] = Set()
    override val underlying: s3.S3AsyncClient = new JavaS3AsyncClient {
      override def serviceName(): String = "s3Recorder"

      override def close(): Unit = ()

      override def listObjectsV2(listObjectsV2Request: ListObjectsV2Request): CompletableFuture[ListObjectsV2Response] = {
        lists += listObjectsV2Request
        CompletableFuture.completedFuture(ListObjectsV2Response.builder().build())
      }

    }
  }
  class RecordingS3Client extends MyAmazonS3 {
    var lists: Set[ListObjectsV2Request] = Set()
    var puts: Set[PutObjectRequest] = Set()

    override def putObject(putObjectRequest: PutObjectRequest): PutObjectResult = {
      puts += putObjectRequest
      val result = new PutObjectResult
      result.setETag("not-null")
      result
    }

  }

}
