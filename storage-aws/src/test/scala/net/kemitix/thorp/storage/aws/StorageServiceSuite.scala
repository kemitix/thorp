package net.kemitix.thorp.storage.aws

import java.time.Instant

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{TransferManager, Upload}
import net.kemitix.thorp.core.{KeyGenerator, Resource, S3MetaDataEnricher}
import net.kemitix.thorp.domain.MD5HashData.Root
import net.kemitix.thorp.domain.StorageQueueEvent.UploadQueueEvent
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class StorageServiceSuite
  extends FunSpec
    with MockFactory {

  val source = Resource(this, "upload")

  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val implLogger: Logger = new DummyLogger
  private val fileToKey = KeyGenerator.generateKey(config.source, config.prefix) _

  describe("getS3Status") {
    val hash = MD5Hash("hash")
    val localFile = LocalFile.resolve("the-file", md5HashMap(hash), source, fileToKey)
    val key = localFile.remoteKey
    val keyOtherKey = LocalFile.resolve("other-key-same-hash", md5HashMap(hash), source, fileToKey)
    val diffHash = MD5Hash("diff")
    val keyDiffHash = LocalFile.resolve("other-key-diff-hash", md5HashMap(diffHash), source, fileToKey)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified), KeyModified(keyOtherKey.remoteKey, lastModified)),
        diffHash -> Set(KeyModified(keyDiffHash.remoteKey, lastModified))),
      byKey = Map(
        key -> HashModified(hash, lastModified),
        keyOtherKey.remoteKey -> HashModified(hash, lastModified),
        keyDiffHash.remoteKey -> HashModified(diffHash, lastModified)))

    def invoke(localFile: LocalFile) =
      S3MetaDataEnricher.getS3Status(localFile, s3ObjectsData)

    def getMatchesByKey(status: (Option[HashModified], Set[(MD5Hash, KeyModified)])): Option[HashModified] = {
      val (byKey, _) = status
      byKey
    }

    def getMatchesByHash(status: (Option[HashModified], Set[(MD5Hash, KeyModified)])): Set[(MD5Hash, KeyModified)] = {
      val (_, byHash) = status
      byHash
    }

    describe("when remote key exists, unmodified and other key matches the hash") {
      it("should return the match by key") {
        val result = getMatchesByKey(invoke(localFile))
        assert(result.contains(HashModified(hash, lastModified)))
      }
      it("should return both matches for the hash") {
        val result = getMatchesByHash(invoke(localFile))
        assertResult(
          Set(
            (hash, KeyModified(key, lastModified)),
            (hash, KeyModified(keyOtherKey.remoteKey, lastModified)))
        )(result)
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val localFile = LocalFile.resolve("missing-file", md5HashMap(MD5Hash("unique")), source, fileToKey)
      it("should return no matches by key") {
        val result = getMatchesByKey(invoke(localFile))
        assert(result.isEmpty)
      }
      it("should return no matches by hash") {
        val result = getMatchesByHash(invoke(localFile))
        assert(result.isEmpty)
      }
    }

    describe("when remote key exists and no others match hash") {
      val localFile = keyDiffHash
      it("should return the match by key") {
        val result = getMatchesByKey(invoke(localFile))
        assert(result.contains(HashModified(diffHash, lastModified)))
      }
      it("should return one match by hash") {
        val result = getMatchesByHash(invoke(localFile))
        assertResult(
          Set(
            (diffHash, KeyModified(keyDiffHash.remoteKey, lastModified)))
        )(result)
      }
    }

  }

  private def md5HashMap(hash: MD5Hash) = {
    Map("md5" -> hash)
  }

  val batchMode: Boolean = true

  describe("upload") {

    describe("when uploading a file") {
      val amazonS3 = stub[AmazonS3]
      val amazonS3TransferManager = stub[TransferManager]
      val storageService = new S3StorageService(amazonS3, amazonS3TransferManager)

      val prefix = RemoteKey("prefix")
      val localFile =
        LocalFile.resolve("root-file", md5HashMap(Root.hash), source, KeyGenerator.generateKey(source, prefix))
      val bucket = Bucket("a-bucket")
      val remoteKey = RemoteKey("prefix/root-file")
      val uploadEventListener = new UploadEventListener(localFile, 1, SyncTotals(), 0L)

      val upload = stub[Upload]
      (amazonS3TransferManager upload (_: PutObjectRequest)).when(*).returns(upload)
      val uploadResult = stub[UploadResult]
      (upload.waitForUploadResult _).when().returns(uploadResult)
      (uploadResult.getETag _).when().returns(Root.hash.hash)
      (uploadResult.getKey _).when().returns(remoteKey.key)

      it("should return hash of uploaded file") {
        pending
        //FIXME: works okay on its own, but fails when run with others
        val expected = UploadQueueEvent(remoteKey, Root.hash)
        val result = storageService.upload(localFile, bucket, batchMode, uploadEventListener, 1)
        assertResult(expected)(result)
      }
    }
  }
}
