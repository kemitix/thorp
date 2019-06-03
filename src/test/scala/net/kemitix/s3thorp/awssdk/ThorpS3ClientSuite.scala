package net.kemitix.s3thorp.awssdk

import java.time.Instant

import cats.effect.IO
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import net.kemitix.s3thorp._
import net.kemitix.s3thorp.domain.Bucket
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, ListObjectsV2Response, S3Object}

import scala.collection.JavaConverters._

class ThorpS3ClientSuite extends FunSpec {

  describe("listObjectsInPrefix") {
    val source = Resource(this, "../upload")
    val prefix = RemoteKey("prefix")
    implicit val config: Config = Config(Bucket("bucket"), prefix, source = source)

    val lm = LastModified(Instant.now)

    val h1 = MD5Hash("hash1")

    val k1a = RemoteKey("key1a")
    val o1a = S3Object.builder.eTag(h1.hash).key(k1a.key).lastModified(lm.when).build

    val k1b = RemoteKey("key1b")
    val o1b = S3Object.builder.eTag(h1.hash).key(k1b.key).lastModified(lm.when).build

    val h2 = MD5Hash("hash2")
    val k2 = RemoteKey("key2")
    val o2 = S3Object.builder.eTag(h2.hash).key(k2.key).lastModified(lm.when).build

    val myFakeResponse: IO[ListObjectsV2Response] = IO {
      ListObjectsV2Response.builder()
        .contents(List(o1a, o1b, o2).asJava)
        .build()
    }
    val amazonS3 = new MyAmazonS3 {}
    val amazonS3TransferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build
    val s3client = new ThorpS3Client(new MyS3CatsIOClient {
      override def listObjectsV2(listObjectsV2Request: ListObjectsV2Request): IO[ListObjectsV2Response] =
        myFakeResponse
    }, amazonS3, amazonS3TransferManager)
    it("should build list of hash lookups, with duplicate objects grouped by hash") {
      val expected = S3ObjectsData(
        byHash = Map(
          h1 -> Set(KeyModified(k1a, lm), KeyModified(k1b, lm)),
          h2 -> Set(KeyModified(k2, lm))),
        byKey = Map(
          k1a -> HashModified(h1, lm),
          k1b -> HashModified(h1, lm),
          k2 -> HashModified(h2, lm)))
      val result: S3ObjectsData = s3client.listObjects(Bucket("bucket"), RemoteKey("prefix")).unsafeRunSync()
      assertResult(expected.byHash.keys)(result.byHash.keys)
      assertResult(expected.byKey.keys)(result.byKey.keys)
      assertResult(expected)(result)
    }
  }

}
