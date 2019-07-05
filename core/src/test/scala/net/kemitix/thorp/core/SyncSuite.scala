package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Paths
import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import net.kemitix.thorp.core.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.MD5HashData.{Leaf, Root}
import net.kemitix.thorp.domain.StorageQueueEvent.{CopyQueueEvent, DeleteQueueEvent, ShutdownQueueEvent, UploadQueueEvent}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, StorageService}
import org.scalatest.FunSpec

class SyncSuite
  extends FunSpec {

  private val source = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val prefix = RemoteKey("prefix")
  private val configOptions = ConfigOptions(List(
    ConfigOption.Source(source.toPath),
    ConfigOption.Bucket("bucket"),
    ConfigOption.Prefix("prefix"),
    ConfigOption.IgnoreGlobalOptions,
    ConfigOption.IgnoreUserOptions
  ))
  implicit private val logger: Logger = new DummyLogger
  private val lastModified = LastModified(Instant.now)

  def putObjectRequest(bucket: Bucket, remoteKey: RemoteKey, localFile: LocalFile): (String, String, File) =
    (bucket.name, remoteKey.key, localFile.file)

  val testBucket = Bucket("bucket")
  // source contains the files root-file and subdir/leaf-file
  val rootRemoteKey = RemoteKey("prefix/root-file")
  val leafRemoteKey = RemoteKey("prefix/subdir/leaf-file")
  val rootFile: LocalFile = LocalFile.resolve("root-file", md5HashMap(Root.hash), sourcePath, _ => rootRemoteKey)

  private def md5HashMap(md5Hash: MD5Hash): Map[String, MD5Hash] = {
    Map("md5" -> md5Hash)
  }

  val leafFile: LocalFile = LocalFile.resolve("subdir/leaf-file", md5HashMap(Leaf.hash), sourcePath, _ => leafRemoteKey)

  val hashService = DummyHashService(Map(
    file("root-file") -> Map("md5" -> MD5HashData.Root.hash),
    file("subdir/leaf-file") -> Map("md5" -> MD5HashData.Leaf.hash)
  ))

  def invokeSubject(storageService: StorageService,
                              hashService: HashService,
                              configOptions: ConfigOptions): Either[List[String], SyncPlan] = {
    PlanBuilder.createPlan(storageService, hashService, configOptions).value.unsafeRunSync
  }

  def invokeSubjectForActions(storageService: StorageService,
                              hashService: HashService,
                              configOptions: ConfigOptions): Either[List[String], Stream[Action]] = {
    invokeSubject(storageService, hashService, configOptions)
      .map(_.actions)
  }

  describe("when all files should be uploaded") {
    val storageService = new RecordingStorageService(testBucket, S3ObjectsData(
      byHash = Map(),
      byKey = Map()))
    it("uploads all files") {
      val expected = Right(Set(
        ToUpload(testBucket, rootFile, rootFile.file.length),
        ToUpload(testBucket, leafFile, leafFile.file.length)))
      val result = invokeSubjectForActions(storageService, hashService, configOptions)
      assertResult(expected)(result.map(_.toSet))
    }
  }

  private def file(filename: String) =
    sourcePath.resolve(Paths.get(filename))

  describe("when no files should be uploaded") {
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        Root.hash -> Set(KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        Leaf.hash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("no actions") {
      val expected = Stream()
      val result = invokeSubjectForActions(storageService, hashService, configOptions)
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
        Root.hash -> Set(KeyModified(sourceKey, lastModified)),
        Leaf.hash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
      byKey = Map(
        sourceKey -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("copies the file and deletes the original") {
      val expected = Stream(
        ToCopy(testBucket,  sourceKey, Root.hash, targetKey, rootFile.file.length),
        ToDelete(testBucket, sourceKey, 0L)
      )
      val result = invokeSubjectForActions(storageService, hashService, configOptions)
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
        Root.hash -> Set(KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        Leaf.hash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified)),
        deletedHash -> Set(KeyModified(RemoteKey("prefix/deleted-file"), lastModified))),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash, lastModified),
        deletedKey -> HashModified(deletedHash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("deleted key") {
      val expected = Stream(
        ToDelete(testBucket, deletedKey, 0L)
      )
      val result = invokeSubjectForActions(storageService,hashService, configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }
  describe("when a file is excluded") {
    val s3ObjectsData = S3ObjectsData(
      byHash = Map(
        Root.hash -> Set(KeyModified(RemoteKey("prefix/root-file"), lastModified)),
        Leaf.hash -> Set(KeyModified(RemoteKey("prefix/subdir/leaf-file"), lastModified))),
      byKey = Map(
        RemoteKey("prefix/root-file") -> HashModified(Root.hash, lastModified),
        RemoteKey("prefix/subdir/leaf-file") -> HashModified(Leaf.hash, lastModified)))
    val storageService = new RecordingStorageService(testBucket, s3ObjectsData)
    it("is not uploaded") {
      val expected = Stream()
      val result = invokeSubjectForActions(storageService, hashService, ConfigOption.Exclude("leaf") :: configOptions)
      assert(result.isRight)
      assertResult(expected)(result.right.get)
    }
  }

  class RecordingStorageService(testBucket: Bucket,
                                s3ObjectsData: S3ObjectsData)
    extends StorageService {

    override def listObjects(bucket: Bucket,
                             prefix: RemoteKey)
                            (implicit l: Logger): EitherT[IO, String, S3ObjectsData] =
      EitherT.liftF(IO.pure(s3ObjectsData))

    override def upload(localFile: LocalFile,
                        bucket: Bucket,
                        batchMode: Boolean,
                        uploadEventListener: UploadEventListener,
                        tryCount: Int): IO[UploadQueueEvent] =
      IO.pure(UploadQueueEvent(localFile.remoteKey, localFile.hashes("md5")))

    override def copy(bucket: Bucket,
                      sourceKey: RemoteKey,
                      hashes: MD5Hash,
                      targetKey: RemoteKey): IO[CopyQueueEvent] =
      IO.pure(CopyQueueEvent(targetKey))

    override def delete(bucket: Bucket,
                        remoteKey: RemoteKey): IO[DeleteQueueEvent] =
      IO.pure(DeleteQueueEvent(remoteKey))

    override def shutdown: IO[StorageQueueEvent] =
      IO.pure(ShutdownQueueEvent())
  }
}
