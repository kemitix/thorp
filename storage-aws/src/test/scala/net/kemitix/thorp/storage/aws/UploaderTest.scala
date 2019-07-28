package net.kemitix.thorp.storage.aws

import java.io.File

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.transfer.model.UploadResult
import net.kemitix.thorp.config.Resource
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.StorageQueueEvent.{
  Action,
  ErrorQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task}

class UploaderTest extends FreeSpec with MockFactory {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  "upload" - {
    val aSource: File = Resource(this, "")
    val aFile: File   = Resource(this, "small-file")
    val aHash         = MD5Hash("aHash")
    val hashes        = Map[HashType, MD5Hash](MD5 -> aHash)
    val remoteKey     = RemoteKey("aRemoteKey")
    val localFile     = LocalFile(aFile, aSource, hashes, remoteKey)
    val bucket        = Bucket("aBucket")
    val batchMode     = false
    val tryCount      = 1
    val uploadResult  = new UploadResult
    uploadResult.setKey(remoteKey.key)
    uploadResult.setETag(aHash.hash)
    val inProgress = new AmazonUpload.InProgress {
      override def waitForUploadResult: UploadResult = uploadResult
    }
    val uploadEventListener =
      UploadEventListener(localFile, 0, SyncTotals(1, 0, 0), 0)
    "when no error" in {
      val expected =
        Right(UploadQueueEvent(remoteKey, aHash))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3TransferManager.upload _)
          .when()
          .returns(_ => Task.succeed(inProgress))
        private val result =
          invoke(fixture.amazonS3TransferManager)(
            localFile,
            bucket,
            batchMode,
            uploadEventListener,
            tryCount
          )
        assertResult(expected)(result)
      }
    }
    "when Amazon Service Exception" in {
      val exception = new AmazonS3Exception("message")
      val expected =
        Right(
          ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, exception))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3TransferManager.upload _)
          .when()
          .returns(_ => Task.fail(exception))
        private val result =
          invoke(fixture.amazonS3TransferManager)(
            localFile,
            bucket,
            batchMode,
            uploadEventListener,
            tryCount
          )
        assertResult(expected)(result)
      }
    }
    "when Amazon SDK Client Exception" in {
      val exception = new SdkClientException("message")
      val expected =
        Right(
          ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, exception))
      new AmazonS3ClientTestFixture {
        (fixture.amazonS3TransferManager.upload _)
          .when()
          .returns(_ => Task.fail(exception))
        private val result =
          invoke(fixture.amazonS3TransferManager)(
            localFile,
            bucket,
            batchMode,
            uploadEventListener,
            tryCount
          )
        assertResult(expected)(result)
      }
    }
    def invoke(transferManager: AmazonTransferManager)(
        localFile: LocalFile,
        bucket: Bucket,
        batchMode: Boolean,
        uploadEventListener: UploadEventListener,
        tryCount: Int
    ) =
      runtime.unsafeRunSync {
        Uploader.upload(transferManager)(
          localFile,
          bucket,
          batchMode,
          uploadEventListener,
          tryCount
        )
      }.toEither
  }

}
