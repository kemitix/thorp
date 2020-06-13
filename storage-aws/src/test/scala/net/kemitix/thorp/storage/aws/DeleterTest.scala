package net.kemitix.thorp.storage.aws

import org.scalatest.FreeSpec

class DeleterTest extends FreeSpec {

//  private val runtime = Runtime(Console.Live, PlatformLive.Default)
//
//  "delete" - {
//    val bucket    = Bucket.named("aBucket")
//    val remoteKey = RemoteKey.create("aRemoteKey")
//    "when no errors" in {
//      val expected = Right(StorageEvent.deleteEvent(remoteKey))
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3Client.deleteObject)
//          .when()
//          .returns(_ => UIO.succeed(()))
//        private val result = invoke(fixture.amazonS3Client)(bucket, remoteKey)
//        assertResult(expected)(result)
//      }
//    }
//    "when Amazon Service Exception" in {
//      val exception = new AmazonS3Exception("message")
//      val expected =
//        Right(
//          StorageEvent.errorEvent(ActionSummary.delete(remoteKey.key),
//                                  remoteKey,
//                                  exception))
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3Client.deleteObject)
//          .when()
//          .returns(_ => Task.fail(exception))
//        private val result = invoke(fixture.amazonS3Client)(bucket, remoteKey)
//        assertResult(expected)(result)
//      }
//    }
//    "when Amazon SDK Client Exception" in {
//      val exception = new SdkClientException("message")
//      val expected =
//        Right(
//          StorageEvent.errorEvent(ActionSummary.delete(remoteKey.key),
//                                  remoteKey,
//                                  exception))
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3Client.deleteObject)
//          .when()
//          .returns(_ => Task.fail(exception))
//        private val result = invoke(fixture.amazonS3Client)(bucket, remoteKey)
//        assertResult(expected)(result)
//      }
//    }
//    def invoke(amazonS3Client: AmazonS3Client.Client)(bucket: Bucket,
//                                                      remoteKey: RemoteKey) =
//      runtime.unsafeRunSync {
//        Deleter.delete(amazonS3Client)(bucket, remoteKey)
//      }.toEither
//
//  }
}
