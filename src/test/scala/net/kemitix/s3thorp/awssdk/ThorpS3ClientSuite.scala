package net.kemitix.s3thorp.awssdk

import java.time.Instant

import scala.collection.JavaConverters._
import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.{Bucket, MD5Hash, RemoteKey}
import org.scalatest.FunSpec
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, ListObjectsV2Response, S3Object}

class ThorpS3ClientSuite extends FunSpec {

  describe("listObjectsInPrefix") {
    val h1 = MD5Hash("hash1")
    val k1 = RemoteKey("key1")
    val lm1 = Instant.now
    val o1 = S3Object.builder.eTag(h1.hash).key(k1.key).lastModified(lm1).build
    val h2 = MD5Hash("hash2")
    val k2 = RemoteKey("key2")
    val lm2 = Instant.now.minusSeconds(200)
    val o2 = S3Object.builder.eTag(h2.hash).key(k2.key).lastModified(lm2).build
    val myFakeResponse: IO[ListObjectsV2Response] = IO{
      ListObjectsV2Response.builder()
        .contents(List(o1, o2).asJava)
        .build()
    }
    val subject = new ThorpS3Client(new S3CatsIOClient {
      override val underlying: S3AsyncClient = new S3AsyncClient {
        override val underlying: s3.S3AsyncClient = new s3.S3AsyncClient {
          override def serviceName(): String = "fake-s3-client"

          override def close(): Unit = ()
        }
      }
      override def listObjectsV2(listObjectsV2Request: ListObjectsV2Request) =
        myFakeResponse
    })
    it("should build list of hash lookups") {
      val result: HashLookup = subject.listObjects(Bucket("bucket"), RemoteKey("prefix")).unsafeRunSync()
      val expected = HashLookup(
        Map(
          h1 -> (k1, lm1),
          h2 -> (k2, lm2)),
        Map(
          k1 -> (h1, lm1),
          k2 -> (h2, lm2)))
      assertResult(expected)(result)
    }
  }

}
