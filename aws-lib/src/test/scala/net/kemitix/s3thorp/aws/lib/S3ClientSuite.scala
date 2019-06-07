package net.kemitix.s3thorp.aws.lib

import java.io.File
import java.time.Instant

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{TransferManager, Upload}
import net.kemitix.s3thorp.aws.api.S3Action.UploadS3Action
import net.kemitix.s3thorp.aws.api.{S3Client, UploadProgressListener}
import net.kemitix.s3thorp.core.{KeyGenerator, MD5HashGenerator, Resource, S3MetaDataEnricher}
import net.kemitix.s3thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class S3ClientSuite
  extends FunSpec
    with MockFactory {

  val source = Resource(this, "upload")

  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logInfo: Int => String => Unit = l => m => ()
  implicit private val logWarn: String => Unit = w => ()
  private val fileToKey = KeyGenerator.generateKey(config.source, config.prefix) _
  private val fileToHash = (file: File) => MD5HashGenerator.md5File(file)

  describe("getS3Status") {
    val hash = MD5Hash("hash")
    val localFile = LocalFile.resolve("the-file", hash, source, fileToKey, fileToHash)
    val key = localFile.remoteKey
    val keyotherkey = LocalFile.resolve("other-key-same-hash", hash, source, fileToKey, fileToHash)
    val diffhash = MD5Hash("diff")
    val keydiffhash = LocalFile.resolve("other-key-diff-hash", diffhash, source, fileToKey, fileToHash)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified), KeyModified(keyotherkey.remoteKey, lastModified)),
        diffhash -> Set(KeyModified(keydiffhash.remoteKey, lastModified))),
      byKey = Map(
        key -> HashModified(hash, lastModified),
        keyotherkey.remoteKey -> HashModified(hash, lastModified),
        keydiffhash.remoteKey -> HashModified(diffhash, lastModified)))

    def invoke(self: S3Client, localFile: LocalFile) = {
      S3MetaDataEnricher.getS3Status(localFile, s3ObjectsData)
    }

    describe("when remote key exists") {
      val s3Client = S3ClientBuilder.defaultClient
      it("should return (Some, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(hash, lastModified)),
            Set(
              KeyModified(key, lastModified),
              KeyModified(keyotherkey.remoteKey, lastModified)))
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val s3Client = S3ClientBuilder.defaultClient
      it("should return (None, Set.empty)") {
        val localFile = LocalFile.resolve("missing-file", MD5Hash("unique"), source, fileToKey, fileToHash)
        assertResult(
          (None,
            Set.empty)
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key exists and no others match hash") {
      val s3Client = S3ClientBuilder.defaultClient
      it("should return (None, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(diffhash, lastModified)),
            Set(KeyModified(keydiffhash.remoteKey, lastModified)))
        )(invoke(s3Client, keydiffhash))
      }
    }

  }

  describe("upload") {

    describe("when uploading a file") {
      val amazonS3 = stub[AmazonS3]
      val amazonS3TransferManager = stub[TransferManager]
      val s3Client = new ThorpS3Client(amazonS3, amazonS3TransferManager)

      val prefix = RemoteKey("prefix")
      val md5Hash = MD5HashGenerator.md5File(source.toPath.resolve("root-file").toFile)
      val localFile: LocalFile = LocalFile.resolve("root-file", md5Hash, source, KeyGenerator.generateKey(source, prefix), fileToHash)
      val bucket: Bucket = Bucket("a-bucket")
      val remoteKey: RemoteKey = RemoteKey("prefix/root-file")
      val progressListener = new UploadProgressListener(localFile)

      val upload = stub[Upload]
      (amazonS3TransferManager upload (_: PutObjectRequest)).when(*).returns(upload)
      val uploadResult = stub[UploadResult]
      (upload.waitForUploadResult _).when().returns(uploadResult)
      (uploadResult.getETag _).when().returns(md5Hash.hash)
      (uploadResult.getKey _).when().returns(remoteKey.key)

      it("should return hash of uploaded file") {
        pending
        //FIXME: works okay on its own, but fails when run with others
        val expected = UploadS3Action(remoteKey, md5Hash)
        val result = s3Client.upload(localFile, bucket, progressListener, config.multiPartThreshold, 1, config.maxRetries).unsafeRunSync
        assertResult(expected)(result)
      }
    }
  }
}
