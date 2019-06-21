package net.kemitix.thorp.core

import java.io.File
import java.time.Instant

import cats.effect.IO
import net.kemitix.thorp.core.MD5HashData.{leafHash, rootHash}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.StorageQueueEvent.{CopyQueueEvent, DeleteQueueEvent, UploadQueueEvent}
import net.kemitix.thorp.storage.api.{StorageService, UploadEventListener}
import org.scalatest.FunSpec

class SyncSuite
  extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  private val configOptions = List(
    ConfigOption.Source(source.toPath),
    ConfigOption.Bucket("bucket"),
    ConfigOption.Prefix("prefix"),
    ConfigOption.IgnoreGlobalOptions,
    ConfigOption.IgnoreUserOptions
  )
  implicit private val logger: Logger = new DummyLogger
  private val lastModified = LastModified(Instant.now)

  def putObjectRequest(bucket: Bucket, remoteKey: RemoteKey, localFile: LocalFile): (String, String, File) =
    (bucket.name, remoteKey.key, localFile.file)

  describe("Sync.apply") {
    val testBucket = Bucket("bucket")
    // source contains the files root-file and subdir/leaf-file
    val rootRemoteKey = RemoteKey("prefix/root-file")
    val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")

    def invokeSubject(storageService: StorageService,
                      configOptions: List[ConfigOption]) = {
      Sync(storageService)(configOptions).unsafeRunSync
    }

    describe("when all files should be uploaded") {
      val storageService = new RecordingStorageService(testBucket, S3ObjectsData(
        byHash = Map(),
        byKey = Map()))
      it("uploads all files") {
        val expectedUploads = Map(
          "subdir/leaf-file" -> leafRemoteKey,
          "root-file" -> rootRemoteKey)
        invokeSubject(storageService, configOptions)
        assertResult(expectedUploads)(storageService.uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        invokeSubject(storageService, configOptions)
        assertResult(expectedCopies)(storageService.copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        invokeSubject(storageService, configOptions)
        assertResult(expectedDeletions)(storageService.deletionsRecord)
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
      val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
      it("uploads nothing") {
        val expectedUploads = Map()
        invokeSubject(storageService, configOptions)
        assertResult(expectedUploads)(storageService.uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        invokeSubject(storageService, configOptions)
        assertResult(expectedCopies)(storageService.copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        invokeSubject(storageService, configOptions)
        assertResult(expectedDeletions)(storageService.deletionsRecord)
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
      val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
      it("uploads nothing") {
        invokeSubject(storageService, configOptions)
        val expectedUploads = Map()
        assertResult(expectedUploads)(storageService.uploadsRecord)
      }
      it("copies the file") {
        val expectedCopies = Map(RemoteKey("prefix/root-file-old") -> RemoteKey("prefix/root-file"))
        invokeSubject(storageService, configOptions)
        assertResult(expectedCopies)(storageService.copiesRecord)
      }
      it("deletes the original") {
        val expectedDeletions = Set(RemoteKey("prefix/root-file-old"))
        invokeSubject(storageService, configOptions)
        assertResult(expectedDeletions)(storageService.deletionsRecord)
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
      val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
      it("deleted key") {
        val expectedDeletions = Set(deletedKey)
        invokeSubject(storageService, configOptions)
        assertResult(expectedDeletions)(storageService.deletionsRecord)
      }
    }
    describe("when a file is excluded") {
      val s3ObjectsData = S3ObjectsData(Map(), Map())
      val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
      it("is not uploaded") {
        val expectedUploads = Map(
          "root-file" -> rootRemoteKey
        )
        invokeSubject(storageService, ConfigOption.Exclude("leaf") :: configOptions)
        assertResult(expectedUploads)(storageService.uploadsRecord)
      }
    }
  }

  class RecordingStorageService(testBucket: Bucket,
                                s3ObjectsData: S3ObjectsData)
    extends StorageService {

    var uploadsRecord: Map[String, RemoteKey] = Map()
    var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
    var deletionsRecord: Set[RemoteKey] = Set()

    override def listObjects(bucket: Bucket,
                             prefix: RemoteKey)
                            (implicit logger: Logger): IO[S3ObjectsData] =
      IO.pure(s3ObjectsData)

    override def upload(localFile: LocalFile,
                        bucket: Bucket,
                        uploadEventListener: UploadEventListener,
                        tryCount: Int)
                       (implicit logger: Logger): IO[UploadQueueEvent] = {
      if (bucket == testBucket)
        uploadsRecord += (localFile.relative.toString -> localFile.remoteKey)
      IO.pure(UploadQueueEvent(localFile.remoteKey, localFile.hash))
    }

    override def copy(bucket: Bucket,
                      sourceKey: RemoteKey,
                      hash: MD5Hash,
                      targetKey: RemoteKey
                     )(implicit logger: Logger): IO[CopyQueueEvent] = {
      if (bucket == testBucket)
        copiesRecord += (sourceKey -> targetKey)
      IO.pure(CopyQueueEvent(targetKey))
    }

    override def delete(bucket: Bucket,
                        remoteKey: RemoteKey
                       )(implicit logger: Logger): IO[DeleteQueueEvent] = {
      if (bucket == testBucket)
        deletionsRecord += remoteKey
      IO.pure(DeleteQueueEvent(remoteKey))
    }
  }
}
