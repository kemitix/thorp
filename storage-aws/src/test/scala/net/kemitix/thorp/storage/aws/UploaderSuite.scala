package net.kemitix.thorp.storage.aws

import java.time.Instant

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer._
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.core.Resource
import net.kemitix.thorp.domain.StorageQueueEvent.UploadQueueEvent
import net.kemitix.thorp.domain.{UploadEventListener, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class UploaderSuite
  extends FunSpec
    with MockFactory {

  private val source = Resource(this, ".")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val implLogger: Logger = new DummyLogger
  private val fileToKey = generateKey(config.source, config.prefix) _
  val lastModified = LastModified(Instant.now())

  def md5HashMap(hash: MD5Hash): Map[String, MD5Hash] =
    Map(
      "md5" -> hash
    )

  val batchMode: Boolean = true

  describe("S3ClientMultiPartTransferManagerSuite") {
    describe("upload") {
      pending
      // how much of this test is testing the amazonTransferManager
      // Should we just test that the correct parameters are passed to initiate, or will this test
      // just collapse and die if the amazonS3 doesn't respond properly to TransferManager input
      // dies when putObject is called
      val returnedKey = RemoteKey("returned-key")
      val returnedHash = MD5Hash("returned-hash")
      val bigFile = LocalFile.resolve("small-file", md5HashMap(MD5Hash("the-hash")), source, fileToKey)
      val uploadEventListener = new UploadEventListener(bigFile, 1, SyncTotals())
      val amazonS3 = mock[AmazonS3]
      val amazonS3TransferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build
      val uploader = new Uploader(amazonS3TransferManager)
      it("should upload") {
        val expected = UploadQueueEvent(returnedKey, returnedHash)
        val result = uploader.upload(bigFile, config.bucket, batchMode, uploadEventListener, 1)
        assertResult(expected)(result)
      }
    }
  }
}
