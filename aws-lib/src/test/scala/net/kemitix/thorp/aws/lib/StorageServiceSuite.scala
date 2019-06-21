package net.kemitix.thorp.aws.lib

import java.time.Instant

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{TransferManager, Upload}
import net.kemitix.thorp.aws.lib.MD5HashData.rootHash
import net.kemitix.thorp.core.{KeyGenerator, Resource, S3MetaDataEnricher}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.StorageQueueEvent.UploadQueueEvent
import net.kemitix.thorp.storage.api.StorageService
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
    val localFile = LocalFile.resolve("the-file", hash, source, fileToKey)
    val key = localFile.remoteKey
    val keyotherkey = LocalFile.resolve("other-key-same-hash", hash, source, fileToKey)
    val diffhash = MD5Hash("diff")
    val keydiffhash = LocalFile.resolve("other-key-diff-hash", diffhash, source, fileToKey)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified), KeyModified(keyotherkey.remoteKey, lastModified)),
        diffhash -> Set(KeyModified(keydiffhash.remoteKey, lastModified))),
      byKey = Map(
        key -> HashModified(hash, lastModified),
        keyotherkey.remoteKey -> HashModified(hash, lastModified),
        keydiffhash.remoteKey -> HashModified(diffhash, lastModified)))

    def invoke(self: StorageService, localFile: LocalFile) = {
      S3MetaDataEnricher.getS3Status(localFile, s3ObjectsData)
    }

    describe("when remote key exists") {
      val storageService = S3StorageServiceBuilder.defaultStorageService
      it("should return (Some, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(hash, lastModified)),
            Set(
              KeyModified(key, lastModified),
              KeyModified(keyotherkey.remoteKey, lastModified)))
        )(invoke(storageService, localFile))
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val storageService = S3StorageServiceBuilder.defaultStorageService
      it("should return (None, Set.empty)") {
        val localFile = LocalFile.resolve("missing-file", MD5Hash("unique"), source, fileToKey)
        assertResult(
          (None,
            Set.empty)
        )(invoke(storageService, localFile))
      }
    }

    describe("when remote key exists and no others match hash") {
      val storageService = S3StorageServiceBuilder.defaultStorageService
      it("should return (None, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(diffhash, lastModified)),
            Set(KeyModified(keydiffhash.remoteKey, lastModified)))
        )(invoke(storageService, keydiffhash))
      }
    }

  }

  describe("upload") {

    describe("when uploading a file") {
      val amazonS3 = stub[AmazonS3]
      val amazonS3TransferManager = stub[TransferManager]
      val storageService = new S3StorageService(amazonS3, amazonS3TransferManager)

      val prefix = RemoteKey("prefix")
      val localFile =
        LocalFile.resolve("root-file", rootHash, source, KeyGenerator.generateKey(source, prefix))
      val bucket = Bucket("a-bucket")
      val remoteKey = RemoteKey("prefix/root-file")
      val uploadEventListener = new UploadEventListener(localFile)

      val upload = stub[Upload]
      (amazonS3TransferManager upload (_: PutObjectRequest)).when(*).returns(upload)
      val uploadResult = stub[UploadResult]
      (upload.waitForUploadResult _).when().returns(uploadResult)
      (uploadResult.getETag _).when().returns(rootHash.hash)
      (uploadResult.getKey _).when().returns(remoteKey.key)

      it("should return hash of uploaded file") {
        pending
        //FIXME: works okay on its own, but fails when run with others
        val expected = UploadQueueEvent(remoteKey, rootHash)
        val result = storageService.upload(localFile, bucket, uploadEventListener, 1)
        assertResult(expected)(result)
      }
    }
  }
}
