package net.kemitix.thorp.storage.aws

import java.io.File

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.transfer.model.UploadResult
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.StorageQueueEvent.{
  Action,
  ErrorQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import zio.{DefaultRuntime, Task}
import net.kemitix.thorp.domain.NonUnit.~*
import net.kemitix.thorp.filesystem.Resource

class UploaderTest extends FreeSpec with MockFactory {

  "upload" - {
    val aSource: File = Resource(this, "")
    val aFile: File   = Resource(this, "small-file")
    val aHash         = MD5Hash("aHash")
    val hashes        = Map[HashType, MD5Hash](MD5 -> aHash)
    val remoteKey     = RemoteKey("aRemoteKey")
    val localFile     = LocalFile(aFile, aSource, hashes, remoteKey)
    val bucket        = Bucket("aBucket")
    val uploadResult  = new UploadResult
    uploadResult.setKey(remoteKey.key)
    uploadResult.setETag(MD5Hash.hash(aHash))
    val inProgress = new AmazonUpload.InProgress {
      override def waitForUploadResult: UploadResult = uploadResult
    }
    val listenerSettings =
      UploadEventListener.Settings(localFile, 0, 0, batchMode = true)
    "when no error" in {
      val expected =
        Right(UploadQueueEvent(remoteKey, aHash))
      new AmazonS3ClientTestFixture {
        ~*(
          (fixture.amazonS3TransferManager.upload _)
            .when()
            .returns(_ => Task.succeed(inProgress)))
        private val result =
          invoke(fixture.amazonS3TransferManager)(
            localFile,
            bucket,
            listenerSettings
          )
        ~*(assertResult(expected)(result))
      }
    }
    "when Amazon Service Exception" in {
      val exception = new AmazonS3Exception("message")
      val expected =
        Right(
          ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, exception))
      new AmazonS3ClientTestFixture {
        ~*(
          (fixture.amazonS3TransferManager.upload _)
            .when()
            .returns(_ => Task.fail(exception)))
        private val result =
          invoke(fixture.amazonS3TransferManager)(
            localFile,
            bucket,
            listenerSettings
          )
        ~*(assertResult(expected)(result))
      }
    }
    "when Amazon SDK Client Exception" in {
      val exception = new SdkClientException("message")
      val expected =
        Right(
          ErrorQueueEvent(Action.Upload(remoteKey.key), remoteKey, exception))
      new AmazonS3ClientTestFixture {
        ~*(
          (fixture.amazonS3TransferManager.upload _)
            .when()
            .returns(_ => Task.fail(exception)))
        private val result =
          invoke(fixture.amazonS3TransferManager)(
            localFile,
            bucket,
            listenerSettings
          )
        ~*(assertResult(expected)(result))
      }
    }
    def invoke(transferManager: AmazonTransferManager)(
        localFile: LocalFile,
        bucket: Bucket,
        listenerSettings: UploadEventListener.Settings
    ) = {
      type TestEnv = Config
      val testEnv: TestEnv = Config.Live
      new DefaultRuntime {}.unsafeRunSync {
        Uploader
          .upload(transferManager)(
            Uploader.Request(localFile, bucket, listenerSettings))
          .provide(testEnv)
      }.toEither
    }
  }

}
