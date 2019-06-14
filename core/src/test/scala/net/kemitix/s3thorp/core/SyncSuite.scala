package net.kemitix.s3thorp.core

import java.io.File
import java.time.Instant

import cats.Id
import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action, UploadS3Action}
import net.kemitix.s3thorp.aws.api.{S3Client, UploadProgressListener}
import net.kemitix.s3thorp.core.MD5HashData.{leafHash, rootHash}
import net.kemitix.s3thorp.domain.Filter.Exclude
import net.kemitix.s3thorp.domain._
import org.scalatest.FunSpec

class SyncSuite
  extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  val config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logger: Logger[Id] = new DummyLogger[Id]
  implicit private val logInfo: Int => String => Id[Unit] = _ => _ => ()
  implicit private val logWarn: String => Id[Unit] = _ => ()
  private val lastModified = LastModified(Instant.now)
  private val fileToKey: File => RemoteKey = KeyGenerator.generateKey(source, prefix)
  private val rootFile = LocalFile.resolve("root-file", rootHash, source, fileToKey)
  private val leafFile = LocalFile.resolve("subdir/leaf-file", leafHash, source, fileToKey)

  private val md5HashGenerator = MD5HashGenerator.md5File[Id](_)

  def putObjectRequest(bucket: Bucket, remoteKey: RemoteKey, localFile: LocalFile): (String, String, File) =
    (bucket.name, remoteKey.key, localFile.file)

  describe("run") {
    val testBucket = Bucket("bucket")
    // source contains the files root-file and subdir/leaf-file
    val rootRemoteKey = RemoteKey("prefix/root-file")
    val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")
    describe("when all files should be uploaded") {
      val s3Client = new RecordingClient(testBucket, S3ObjectsData(
        byHash = Map(),
        byKey = Map()))
      Sync.run(config, s3Client, md5HashGenerator, logger)
      it("uploads all files") {
        val expectedUploads = Map(
          "subdir/leaf-file" -> leafRemoteKey,
          "root-file" -> rootRemoteKey
        )
        assertResult(expectedUploads)(s3Client.uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        assertResult(expectedCopies)(s3Client.copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        assertResult(expectedDeletions)(s3Client.deletionsRecord)
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
      val s3Client = new RecordingClient(testBucket, s3ObjectsData)
      Sync.run(config, s3Client, md5HashGenerator, logger)
      it("uploads nothing") {
        val expectedUploads = Map()
        assertResult(expectedUploads)(s3Client.uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        assertResult(expectedCopies)(s3Client.copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        assertResult(expectedDeletions)(s3Client.deletionsRecord)
      }
    }
    describe("when a file is renamed it is moved on S3 with no upload") {
      // 'root-file-old' should be renamed as 'root-file'
      val s3ObjectsData = S3ObjectsData(
        byHash = Map(
          rootHash -> Set(KeyModified(RemoteKey("prefix/root-file-old"), lastModified)),
          leafHash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
        byKey = Map(
          RemoteKey("prefix/root-file-old") -> HashModified(rootHash, lastModified),
          RemoteKey("prefix/subdir/leaf-file") -> HashModified(leafHash, lastModified)))
      val s3Client = new RecordingClient(testBucket, s3ObjectsData)
      Sync.run(config, s3Client, md5HashGenerator, logger)
      it("uploads nothing") {
        val expectedUploads = Map()
        assertResult(expectedUploads)(s3Client.uploadsRecord)
      }
      it("copies the file") {
        val expectedCopies = Map(RemoteKey("prefix/root-file-old") -> RemoteKey("prefix/root-file"))
        assertResult(expectedCopies)(s3Client.copiesRecord)
      }
      it("deletes the original") {
        val expectedDeletions = Set(RemoteKey("prefix/root-file-old"))
        assertResult(expectedDeletions)(s3Client.deletionsRecord)
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
      val s3Client = new RecordingClient(testBucket, s3ObjectsData)
      Sync.run(config, s3Client, md5HashGenerator, logger)
      it("deleted key") {
        val expectedDeletions = Set(deletedKey)
        assertResult(expectedDeletions)(s3Client.deletionsRecord)
      }
    }
    describe("when a file is excluded") {
      val config: Config = Config(Bucket("bucket"), prefix, source = source, filters = List(Exclude("leaf")))
      val s3ObjectsData = S3ObjectsData(Map(), Map())
      val s3Client = new RecordingClient(testBucket, s3ObjectsData)
      Sync.run(config, s3Client, md5HashGenerator, logger)
      it("is not uploaded") {
        val expectedUploads = Map(
          "root-file" -> rootRemoteKey
        )
        assertResult(expectedUploads)(s3Client.uploadsRecord)
      }
    }
  }

  class RecordingClient(testBucket: Bucket,
                        s3ObjectsData: S3ObjectsData)
    extends S3Client[Id] {

    var uploadsRecord: Map[String, RemoteKey] = Map()
    var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
    var deletionsRecord: Set[RemoteKey] = Set()

    override def listObjects(bucket: Bucket,
                             prefix: RemoteKey)
                            (implicit logger: Logger[Id]): S3ObjectsData =
      s3ObjectsData

    override def upload(localFile: LocalFile,
                        bucket: Bucket,
                        progressListener: UploadProgressListener,
                        multiPartThreshold: Long,
                        tryCount: Int,
                        maxRetries: Int)
                       (implicit logger: Logger[Id]): UploadS3Action = {
      if (bucket == testBucket)
        uploadsRecord += (localFile.relative.toString -> localFile.remoteKey)
      UploadS3Action(localFile.remoteKey, MD5Hash("some hash value"))
    }

    override def copy(bucket: Bucket,
                      sourceKey: RemoteKey,
                      hash: MD5Hash,
                      targetKey: RemoteKey
                     )(implicit info: Int => String => Id[Unit]): CopyS3Action = {
      if (bucket == testBucket)
        copiesRecord += (sourceKey -> targetKey)
      CopyS3Action(targetKey)
    }

    override def delete(bucket: Bucket,
                        remoteKey: RemoteKey
                       )(implicit logger: Logger[Id]): DeleteS3Action = {
      if (bucket == testBucket)
        deletionsRecord += remoteKey
      DeleteS3Action(remoteKey)
    }
  }
}
