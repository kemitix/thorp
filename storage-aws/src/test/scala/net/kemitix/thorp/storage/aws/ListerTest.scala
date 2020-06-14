package net.kemitix.thorp.storage.aws

import org.scalatest.FreeSpec

class ListerTest extends FreeSpec {

//  "list" - {
//    val bucket = Bucket.named("aBucket")
//    val prefix = RemoteKey.create("aRemoteKey")
//    "when no errors" - {
//      "when single fetch required" in {
//        val nowDate         = new Date
//        val key             = "key"
//        val etag            = "etag"
//        val expectedHashMap = Map(MD5Hash.create(etag) -> RemoteKey.create(key))
//        val expectedKeyMap  = Map(RemoteKey.create(key) -> MD5Hash.create(etag))
//        new AmazonS3ClientTestFixture {
//          (() => fixture.amazonS3Client.listObjectsV2)
//            .when()
//            .returns(_ => {
//              UIO.succeed(objectResults(nowDate, key, etag, truncated = false))
//            })
//          private val result = invoke(fixture.amazonS3Client)(bucket, prefix)
//          private val hashMap =
//            result.map(_.byHash).map(m => Map.from(m.asMap.asScala))
//          private val keyMap =
//            result.map(_.byKey).map(m => Map.from(m.asMap.asScala))
//          hashMap should be(Right(expectedHashMap))
//          keyMap should be(Right(expectedKeyMap))
//        }
//      }
//
//      "when second fetch required" in {
//        val nowDate = new Date
//        val key1    = "key1"
//        val etag1   = "etag1"
//        val key2    = "key2"
//        val etag2   = "etag2"
//        val expectedHashMap = Map(
//          MD5Hash.create(etag1) -> RemoteKey.create(key1),
//          MD5Hash.create(etag2) -> RemoteKey.create(key2)
//        )
//        val expectedKeyMap = Map(
//          RemoteKey.create(key1) -> MD5Hash.create(etag1),
//          RemoteKey.create(key2) -> MD5Hash.create(etag2)
//        )
//        new AmazonS3ClientTestFixture {
//
//          (() => fixture.amazonS3Client.listObjectsV2)
//            .when()
//            .returns(_ =>
//              UIO(objectResults(nowDate, key1, etag1, truncated = true)))
//            .noMoreThanOnce()
//
//          (() => fixture.amazonS3Client.listObjectsV2)
//            .when()
//            .returns(_ =>
//              UIO(objectResults(nowDate, key2, etag2, truncated = false)))
//          private val result = invoke(fixture.amazonS3Client)(bucket, prefix)
//          private val hashMap =
//            result.map(_.byHash).map(m => Map.from(m.asMap.asScala))
//          private val keyMap =
//            result.map(_.byKey).map(m => Map.from(m.asMap.asScala))
//          hashMap should be(Right(expectedHashMap))
//          keyMap should be(Right(expectedKeyMap))
//        }
//      }
//
//      def objectSummary(key: String, etag: String, lastModified: Date) = {
//        val objectSummary = new S3ObjectSummary
//        objectSummary.setKey(key)
//        objectSummary.setETag(etag)
//        objectSummary.setLastModified(lastModified)
//        objectSummary
//      }
//
//      def objectResults(nowDate: Date,
//                        key: String,
//                        etag: String,
//                        truncated: Boolean) = {
//        val result = new ListObjectsV2Result
//        result.getObjectSummaries.add(objectSummary(key, etag, nowDate))
//        result.setTruncated(truncated)
//        result
//      }
//
//    }
//    "when Amazon Service Exception" in {
//      val exception = new AmazonS3Exception("message")
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3Client.listObjectsV2)
//          .when()
//          .returns(_ => Task.fail(exception))
//        private val result = invoke(fixture.amazonS3Client)(bucket, prefix)
//        assert(result.isLeft)
//      }
//    }
//    "when Amazon SDK Client Exception" in {
//      val exception = new SdkClientException("message")
//      new AmazonS3ClientTestFixture {
//        (() => fixture.amazonS3Client.listObjectsV2)
//          .when()
//          .returns(_ => Task.fail(exception))
//        private val result = invoke(fixture.amazonS3Client)(bucket, prefix)
//        assert(result.isLeft)
//      }
//    }
//    def invoke(amazonS3Client: AmazonS3Client.Client)(bucket: Bucket,
//                                                      prefix: RemoteKey) = {
//      object TestEnv extends Storage.Test with Console.Test
//      val program: RIO[Storage with Console, RemoteObjects] = Lister
//        .listObjects(amazonS3Client)(bucket, prefix)
//      val runtime = new DefaultRuntime {}
//      runtime.unsafeRunSync(program.provide(TestEnv)).toEither
//    }
//
//  }

}
