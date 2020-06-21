package net.kemitix.thorp.storage.aws

import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec

class UploaderTest extends FreeSpec with MockFactory {

//  val uiChannel: UChannel[Any, UIEvent] = zioMessage => ()
//
//  "upload" - {
//    val aSource: File = Resource(this, "").toFile
//    val aFile: File   = Resource(this, "small-file").toFile
//    val aHash         = MD5Hash.create("aHash")
//    val hashes        = Hashes.create(MD5, aHash)
//    val remoteKey     = RemoteKey.create("aRemoteKey")
//    val localFile =
//      LocalFile.create(aFile, aSource, hashes, remoteKey, aFile.length)
//    val bucket       = Bucket.named("aBucket")
//    val uploadResult = new UploadResult
//    uploadResult.setKey(remoteKey.key)
//    uploadResult.setETag(aHash.hash())
//    val listenerSettings =
//      UploadEventListener.Settings(uiChannel, localFile, 0, 0, batchMode = true)
//    "when no error" in {
//      val expected =
//        Right(StorageEvent.uploadEvent(remoteKey, aHash))
//      val inProgress = new AmazonUpload.InProgress {
//        override def waitForUploadResult: Task[UploadResult] =
//          Task(uploadResult)
//      }
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3TransferManager.uploader)
//          .when()
//          .returns(_ => UIO.succeed(inProgress))
//        private val result =
//          invoke(fixture.amazonS3TransferManager)(
//            localFile,
//            bucket,
//            listenerSettings
//          )
//        assertResult(expected)(result)
//      }
//    }
//    "when Amazon Service Exception" in {
//      val exception = new AmazonS3Exception("message")
//      val expected =
//        Right(
//          StorageEvent.errorEvent(ActionSummary.upload(remoteKey.key),
//                                  remoteKey,
//                                  exception))
//      val inProgress = new AmazonUpload.InProgress {
//        override def waitForUploadResult: Task[UploadResult] =
//          Task.fail(exception)
//      }
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3TransferManager.upload)
//          .when()
//          .returns(_ => UIO.succeed(inProgress))
//        private val result =
//          invoke(fixture.amazonS3TransferManager)(
//            localFile,
//            bucket,
//            listenerSettings
//          )
//        assertResult(expected)(result)
//      }
//    }
//    "when Amazon SDK Client Exception" in {
//      val exception = new SdkClientException("message")
//      val expected =
//        Right(
//          StorageEvent.errorEvent(ActionSummary.upload(remoteKey.key),
//                                  remoteKey,
//                                  exception))
//      val inProgress = new AmazonUpload.InProgress {
//        override def waitForUploadResult: Task[UploadResult] =
//          Task.fail(exception)
//      }
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3TransferManager.upload)
//          .when()
//          .returns(_ => UIO.succeed(inProgress))
//        private val result =
//          invoke(fixture.amazonS3TransferManager)(
//            localFile,
//            bucket,
//            listenerSettings
//          )
//        assertResult(expected)(result)
//      }
//    }
//    def invoke(transferManager: AmazonTransferManager)(
//        localFile: LocalFile,
//        bucket: Bucket,
//        listenerSettings: UploadEventListener.Settings
//    ) = {
//      val program = Uploader
//        .upload(transferManager)(
//          Uploader.Request(localFile, bucket, listenerSettings))
//      val runtime = new DefaultRuntime {}
//      runtime
//        .unsafeRunSync(
//          program
//            .provide(Config.Live))
//        .toEither
//    }
//  }

}
