package net.kemitix.s3thorp.awssdk

import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.collection.JavaConverters._
import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, HashModified, KeyModified, LastModified, MD5Hash, RemoteKey}
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, ListObjectsV2Response, S3Object}

class ThorpS3ClientSuite extends FunSpec {

  describe("listObjectsInPrefix") {
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
    val s3client = new ThorpS3Client(new MyS3CatsIOClient {
      override def listObjectsV2(listObjectsV2Request: ListObjectsV2Request) =
        myFakeResponse
    })
    it("should build list of hash lookups, with duplicate objects grouped by hash") {
      val expected = S3ObjectsData(
        byHash = Map(
          h1 -> Set(KeyModified(k1a, lm), KeyModified(k1b, lm)),
          h2 -> Set(KeyModified(k2, lm))),
        byKey = Map(
          k1a -> HashModified(h1, lm),
          k2 -> HashModified(h2, lm)))
      val result: S3ObjectsData = s3client.listObjects(Bucket("bucket"), RemoteKey("prefix")).unsafeRunSync()
      assertResult(expected.byHash.keys)(result.byHash.keys)
      assertResult(expected.byKey.keys)(result.byKey.keys)
      assertResult(expected)(result)
    }
  }
  trait MyS3CatsIOClient extends S3CatsIOClient {
    override val underlying: S3AsyncClient = new S3AsyncClient {
      override val underlying: s3.S3AsyncClient = new s3.S3AsyncClient {
        override def serviceName(): String = "fake-s3-client"

        override def close(): Unit = ()
      }
    }
  }
}
