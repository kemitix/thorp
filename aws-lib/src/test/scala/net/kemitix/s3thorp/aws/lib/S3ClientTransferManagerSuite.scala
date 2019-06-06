package net.kemitix.s3thorp.aws.lib

import java.io.File
import java.time.Instant

import com.amazonaws.AmazonClientException
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.model
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer._
import net.kemitix.s3thorp.aws.api.S3Action.UploadS3Action
import net.kemitix.s3thorp.aws.api.UploadProgressListener
import net.kemitix.s3thorp.core.KeyGenerator.generateKey
import net.kemitix.s3thorp.core.{MD5HashGenerator, Resource}
import net.kemitix.s3thorp.domain._
import org.scalatest.FunSpec

class S3ClientTransferManagerSuite
  extends FunSpec {

  private val source = Resource(this, ".")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logInfo: Int => String => Unit = l => m => ()
  implicit private val logWarn: String => Unit = w => ()
  private val fileToKey = generateKey(config.source, config.prefix) _
  private val fileToHash = (file: File) => MD5HashGenerator.md5File(file)
  val lastModified = LastModified(Instant.now())

  describe("S3ClientMultiPartTransferManagerSuite") {
    describe("accepts") {
      val transferManager = new MyTransferManager(("", "", new File("")), RemoteKey(""), MD5Hash(""))
      val uploader = new S3ClientTransferManager(transferManager)
      describe("small-file") {
        val smallFile = LocalFile.resolve("small-file", MD5Hash("the-hash"), source, fileToKey, fileToHash)
        it("should be a small-file") {
          assert(smallFile.file.length < 5 * 1024 * 1024)
        }
        it("should not accept small-file") {
          assertResult(false)(uploader.accepts(smallFile)(config.multiPartThreshold))
        }
      }
      describe("big-file") {
        val bigFile = LocalFile.resolve("big-file", MD5Hash("the-hash"), source, fileToKey, fileToHash)
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
      val bigFile = LocalFile.resolve("small-file", MD5Hash("the-hash"), source, fileToKey, fileToHash)
      val progressListener = new UploadProgressListener(bigFile)
      val amazonS3 = new MyAmazonS3 {}
      val amazonS3TransferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build
        new MyTransferManager(
        signature = (config.bucket.name, bigFile.remoteKey.key, bigFile.file),
        returnedKey = returnedKey,
        returnedHash = returnedHash)
      val uploader = new S3ClientTransferManager(amazonS3TransferManager)
      it("should upload") {
        val expected = UploadS3Action(returnedKey, returnedHash)
        val result = uploader.upload(bigFile, config.bucket, progressListener, config.multiPartThreshold, 1, config.maxRetries).unsafeRunSync
        assertResult(expected)(result)
      }
    }
  }

  class MyTransferManager(signature: (String, String, File),
                          returnedKey: RemoteKey,
                          returnedHash: MD5Hash) extends TransferManager {
    override def upload(bucketName: String, key: String, file: File): Upload = {
      if ((bucketName, key, file) == signature) {
        new MyUpload {
          override def waitForUploadResult(): UploadResult = {
            val result = new UploadResult()
            result.setBucketName(bucketName)
            result.setETag(returnedHash.hash)
            result.setKey(returnedKey.key)
            result.setVersionId("version-id")
            result
          }
        }
      } else new MyUpload
    }
  }
  class MyUpload extends Upload {

    override def waitForUploadResult(): UploadResult = ???

    override def pause(): PersistableUpload = ???

    override def tryPause(forceCancelTransfers: Boolean): PauseResult[PersistableUpload] = ???

    override def abort(): Unit = ???

    override def isDone: Boolean = ???

    override def waitForCompletion(): Unit = ???

    override def waitForException(): AmazonClientException = ???

    override def getDescription: String = ???

    override def getState: Transfer.TransferState = ???

    override def getProgress: TransferProgress = ???

    override def addProgressListener(listener: ProgressListener): Unit = ???

    override def removeProgressListener(listener: ProgressListener): Unit = ???

    override def addProgressListener(listener: model.ProgressListener): Unit = ???

    override def removeProgressListener(listener: model.ProgressListener): Unit = ???
  }

}
