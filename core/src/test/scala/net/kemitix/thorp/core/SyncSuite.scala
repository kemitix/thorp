package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Paths
import java.time.Instant

import net.kemitix.thorp.console
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.core.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.MD5HashData.{Leaf, Root}
import net.kemitix.thorp.domain.StorageQueueEvent.{
  CopyQueueEvent,
  DeleteQueueEvent,
  ShutdownQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, Storage}
import org.scalatest.FunSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task, TaskR}

class SyncSuite extends FunSpec {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  private val testBucket = Bucket("bucket")
  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  // source contains the files root-file and subdir/leaf-file
  private val rootRemoteKey = RemoteKey("prefix/root-file")
  private val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")
  private val rootFile: LocalFile =
    LocalFile.resolve("root-file",
                      md5HashMap(Root.hash),
                      sourcePath,
                      _ => rootRemoteKey)
  private val leafFile: LocalFile =
    LocalFile.resolve("subdir/leaf-file",
                      md5HashMap(Leaf.hash),
                      sourcePath,
                      _ => leafRemoteKey)
  private val hashService =
    DummyHashService(
      Map(
        file("root-file")        -> Map(MD5 -> MD5HashData.Root.hash),
        file("subdir/leaf-file") -> Map(MD5 -> MD5HashData.Leaf.hash)
      ))
  private val configOptions =
    ConfigOptions(
      List(
        ConfigOption.Source(sourcePath),
        ConfigOption.Bucket("bucket"),
        ConfigOption.Prefix("prefix"),
        ConfigOption.IgnoreGlobalOptions,
        ConfigOption.IgnoreUserOptions
      ))
  private val lastModified = LastModified(Instant.now)

  def putObjectRequest(bucket: Bucket,
                       remoteKey: RemoteKey,
                       localFile: LocalFile): (String, String, File) =
    (bucket.name, remoteKey.key, localFile.file)

  private def md5HashMap(md5Hash: MD5Hash): Map[HashType, MD5Hash] =
    Map(MD5 -> md5Hash)

  private def file(filename: String) =
    sourcePath.resolve(Paths.get(filename))

  describe("when all files should be uploaded") {
    val storageService =
      new RecordingStorageService(testBucket, S3ObjectsData())
    it("uploads all files") {
      val expected = Right(
        Set(ToUpload(testBucket, rootFile, rootFile.file.length),
            ToUpload(testBucket, leafFile, leafFile.file.length)))
      val result =
        invokeSubjectForActions(storageService, hashService, configOptions)
      assertResult(expected)(result.map(_.toSet))
    }
  }

  describe("when no files should be uploaded") {
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        Root.hash -> Set(
          KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        Leaf.hash -> Set(
          KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))
      ),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash,
                                                             lastModified))
    )
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("no actions") {
      val expected = Stream()
      val result =
        invokeSubjectForActions(storageService, hashService, configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }
  describe("when a file is renamed it is moved on S3 with no upload") {
    val sourceKey = RemoteKey("prefix/root-file-old")
    val targetKey = RemoteKey("prefix/root-file")
    // 'root-file-old' should be renamed as 'root-file'
    val s3ObjectsData = S3ObjectsData(
      byHash =
        Map(Root.hash -> Set(KeyModified(sourceKey, lastModified)),
            Leaf.hash -> Set(
              KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
      byKey =
        Map(sourceKey -> HashModified(Root.hash, lastModified),
            RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash,
                                                                 lastModified))
    )
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("copies the file and deletes the original") {
      val expected = Stream(
        ToCopy(testBucket,
               sourceKey,
               Root.hash,
               targetKey,
               rootFile.file.length),
        ToDelete(testBucket, sourceKey, 0L)
      )
      val result =
        invokeSubjectForActions(storageService, hashService, configOptions)
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
    val deletedKey  = RemoteKey("prefix/deleted-file")
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        Root.hash -> Set(
          KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        Leaf.hash -> Set(
          KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified)),
        deletedHash -> Set(
          KeyModified(RemoteKey("prefix/deleted-file"), lastModified))
      ),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash,
                                                             lastModified),
        deletedKey -> HashModified(deletedHash, lastModified)
      )
    )
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("deleted key") {
      val expected = Stream(
        ToDelete(testBucket, deletedKey, 0L)
      )
      val result =
        invokeSubjectForActions(storageService, hashService, configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }
  describe("when a file is excluded") {
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        Root.hash -> Set(
          KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        Leaf.hash -> Set(
          KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))
      ),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash,
                                                             lastModified))
    )
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("is not uploaded") {
      val expected = Stream()
      val result =
        invokeSubjectForActions(storageService,
                                hashService,
                                ConfigOption.Exclude("leaf") :: configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }

  class RecordingStorageService(testBucket: Bucket,
                                s3ObjectsData: S3ObjectsData)
      extends Storage.Service {

    override def listObjects(
        bucket: Bucket,
        prefix: RemoteKey): TaskR[console.Console, S3ObjectsData] =
      TaskR(s3ObjectsData)

    override def upload(localFile: LocalFile,
                        bucket: Bucket,
                        batchMode: Boolean,
                        uploadEventListener: UploadEventListener,
                        tryCount: Int): Task[UploadQueueEvent] =
      Task(UploadQueueEvent(localFile.remoteKey, localFile.hashes(MD5)))

    override def copy(bucket: Bucket,
                      sourceKey: RemoteKey,
                      hashes: MD5Hash,
                      targetKey: RemoteKey): Task[CopyQueueEvent] =
      Task(CopyQueueEvent(sourceKey, targetKey))

    override def delete(bucket: Bucket,
                        remoteKey: RemoteKey): Task[DeleteQueueEvent] =
      Task(DeleteQueueEvent(remoteKey))

    override def shutdown: Task[StorageQueueEvent] =
      Task(ShutdownQueueEvent())
  }

  def invokeSubjectForActions(
      storageService: Storage.Service,
      hashService: HashService,
      configOptions: ConfigOptions): Either[Any, Stream[Action]] = {
    invoke(storageService, hashService, configOptions)
      .map(_.actions)
  }

  def invoke(storageService: Storage.Service,
             hashService: HashService,
             configOptions: ConfigOptions): Either[Any, SyncPlan] = {
    runtime.unsafeRunSync {
      PlanBuilder
        .createPlan(storageService, hashService, configOptions)
    }.toEither
  }
}
