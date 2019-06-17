package net.kemitix.thorp.aws.lib

import java.time.Instant

import cats.Id
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer._
import net.kemitix.s3thorp.aws.api.S3Action.UploadS3Action
import net.kemitix.s3thorp.aws.api.UploadProgressListener
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.core.Resource
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class UploaderSuite
  extends FunSpec
    with MockFactory {

  private val source = Resource(this, ".")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val implLogger: Logger[Id] = new DummyLogger[Id]
  private val fileToKey = generateKey(config.source, config.prefix) _
  val lastModified = LastModified(Instant.now())

  describe("S3ClientMultiPartTransferManagerSuite") {
    describe("accepts") {
      val transferManager = stub[TransferManager]
      val uploader = new Uploader(transferManager)
      describe("small-file") {
        val smallFile = LocalFile.resolve("small-file", MD5Hash("the-hash"), source, fileToKey)
        it("should be a small-file") {
          assert(smallFile.file.length < 5 * 1024 * 1024)
        }
        it("should not accept small-file") {
          assertResult(false)(uploader.accepts(smallFile)(config.multiPartThreshold))
        }
      }
      describe("big-file") {
        val bigFile = LocalFile.resolve("big-file", MD5Hash("the-hash"), source, fileToKey)
        it("should be a big-file") {
          assert(bigFile.file.length > 5 * 1024 * 1024)
        }
        it("should accept big-file") {
          assertResult(true)(uploader.accepts(bigFile)(config.multiPartThreshold))
        }
      }
    }
    describe("upload") {
      pending
      // how much of this test is testing the amazonTransferManager
      // Should we just test that the correct parameters are passed to initiate, or will this test
      // just collapse and die if the amazonS3 doesn't respond properly to TransferManager input
      // dies when putObject is called
      val returnedKey = RemoteKey("returned-key")
      val returnedHash = MD5Hash("returned-hash")
      val bigFile = LocalFile.resolve("small-file", MD5Hash("the-hash"), source, fileToKey)
      val progressListener = new UploadProgressListener(bigFile)
      val amazonS3 = mock[AmazonS3]
      val amazonS3TransferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build
      val uploader = new Uploader(amazonS3TransferManager)
      it("should upload") {
        val expected = UploadS3Action(returnedKey, returnedHash)
        val result = uploader.upload(bigFile, config.bucket, progressListener, config.multiPartThreshold, 1, config.maxRetries)
        assertResult(expected)(result)
      }
    }
  }
}
