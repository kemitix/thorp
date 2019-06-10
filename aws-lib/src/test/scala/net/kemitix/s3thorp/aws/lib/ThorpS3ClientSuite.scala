package net.kemitix.s3thorp.aws.lib

import java.time.Instant
import java.util.Date

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, ListObjectsV2Result, S3ObjectSummary}
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.s3thorp.core.Resource
import net.kemitix.s3thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class ThorpS3ClientSuite
  extends FunSpec
    with MockFactory {

  describe("listObjectsInPrefix") {
    val source = Resource(this, "upload")
    val prefix = RemoteKey("prefix")
    implicit val config: Config = Config(Bucket("bucket"), prefix, source = source)
    implicit val logInfo: Int => String => IO[Unit] = _ => _ => IO.unit

    val lm = LastModified(Instant.now)

    val h1 = MD5Hash("hash1")

    val k1a = RemoteKey("key1a")

    def objectSummary(hash: MD5Hash, remoteKey: RemoteKey, lastModified: LastModified) = {
      val summary = new S3ObjectSummary()
      summary.setETag(hash.hash)
      summary.setKey(remoteKey.key)
      summary.setLastModified(Date.from(lastModified.when))
      summary
    }

    val o1a = objectSummary(h1, k1a, lm)

    val k1b = RemoteKey("key1b")
    val o1b = objectSummary(h1, k1b, lm)

    val h2 = MD5Hash("hash2")
    val k2 = RemoteKey("key2")
    val o2 = objectSummary(h2, k2, lm)

    val amazonS3 = stub[AmazonS3]
    val amazonS3TransferManager = stub[TransferManager]
    val s3Client = new ThorpS3Client(amazonS3, amazonS3TransferManager)

    val myFakeResponse = new ListObjectsV2Result()
    val summaries = myFakeResponse.getObjectSummaries
    summaries.add(o1a)
    summaries.add(o1b)
    summaries.add(o2)
    (amazonS3 listObjectsV2 (_: ListObjectsV2Request)).when(*).returns(myFakeResponse)

    it("should build list of hash lookups, with duplicate objects grouped by hash") {
      val expected = S3ObjectsData(
        byHash = Map(
          h1 -> Set(KeyModified(k1a, lm), KeyModified(k1b, lm)),
          h2 -> Set(KeyModified(k2, lm))),
        byKey = Map(
          k1a -> HashModified(h1, lm),
          k1b -> HashModified(h1, lm),
          k2 -> HashModified(h2, lm)))
      val result = s3Client.listObjects(Bucket("bucket"), RemoteKey("prefix")).unsafeRunSync
      assertResult(expected.byHash.keys)(result.byHash.keys)
      assertResult(expected.byKey.keys)(result.byKey.keys)
      assertResult(expected)(result)
    }
  }

}
