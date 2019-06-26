package net.kemitix.thorp.core

import java.io.File
import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import net.kemitix.thorp.core.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.core.MD5HashData.{leafHash, rootHash}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.domain.StorageQueueEvent.{CopyQueueEvent, DeleteQueueEvent, UploadQueueEvent}
import net.kemitix.thorp.storage.api.StorageService
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

  val testBucket = Bucket("bucket")
  // source contains the files root-file and subdir/leaf-file
  val rootRemoteKey = RemoteKey("prefix/root-file")
  val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")
  val rootFile: LocalFile = LocalFile.resolve("root-file", rootHash, source, _ => rootRemoteKey)
  val leafFile: LocalFile = LocalFile.resolve("subdir/leaf-file", leafHash, source, _ => leafRemoteKey)

  def invokeSubject(storageService: StorageService,
                    configOptions: List[ConfigOption]): Either[List[String], Stream[Action]] = {
    Synchronise(storageService, configOptions).value.unsafeRunSync
  }

  describe("when all files should be uploaded") {
    val storageService = new RecordingStorageService(testBucket, S3ObjectsData(
      byHash = Map(),
      byKey = Map()))
    it("uploads all files") {
      val expected = Right(Set(
        ToUpload(testBucket, rootFile),
        ToUpload(testBucket, leafFile)))
      val result = invokeSubject(storageService, configOptions)
      assertResult(expected)(result.map(_.toSet))
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
    it("no actions") {
      val expected = Stream()
      val result = invokeSubject(storageService, configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }
  describe("when a file is renamed it is moved on S3 with no upload") {
    val sourceKey = RemoteKey("prefix/root-file-old")
    val targetKey = RemoteKey("prefix/root-file")
    // 'root-file-old' should be renamed as 'root-file'
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        rootHash -> Set(KeyModified(sourceKey, lastModified)),
        leafHash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
      byKey = Map(
        sourceKey -> HashModified(rootHash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(leafHash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("copies the file and deletes the original") {
      val expected = Stream(
        ToCopy(testBucket,  sourceKey, rootHash, targetKey),
        ToDelete(testBucket, sourceKey)
      )
      val result = invokeSubject(storageService, configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
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
        rootHash -> Set(KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        leafHash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified)),
        deletedHash -> Set(KeyModified(RemoteKey("prefix/deleted-file"), lastModified))),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(rootHash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(leafHash, lastModified),
        deletedKey -> HashModified(deletedHash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("deleted key") {
      val expected = Stream(
        ToDelete(testBucket, deletedKey)
      )
      val result = invokeSubject(storageService, configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }
  describe("when a file is excluded") {
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        rootHash -> Set(KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        leafHash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(rootHash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(leafHash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("is not uploaded") {
      val expected = Stream()
      val result = invokeSubject(storageService, ConfigOption.Exclude("leaf") :: configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }

  class RecordingStorageService(testBucket: Bucket,
                                s3ObjectsData: S3ObjectsData)
    extends StorageService {

    override def listObjects(bucket: Bucket,
                             prefix: RemoteKey): EitherT[IO, String, S3ObjectsData] =
      EitherT.liftF(IO.pure(s3ObjectsData))

    override def upload(localFile: LocalFile,
                        bucket: Bucket,
                        uploadEventListener: UploadEventListener,
                        tryCount: Int): IO[UploadQueueEvent] = {
      IO.pure(UploadQueueEvent(localFile.remoteKey, localFile.hash))
    }

    override def copy(bucket: Bucket,
                      sourceKey: RemoteKey,
                      hash: MD5Hash,
                      targetKey: RemoteKey): IO[CopyQueueEvent] = {
      IO.pure(CopyQueueEvent(targetKey))
    }

    override def delete(bucket: Bucket,
                        remoteKey: RemoteKey): IO[DeleteQueueEvent] = {
      IO.pure(DeleteQueueEvent(remoteKey))
    }
  }
}
