package net.kemitix.thorp.storage.aws

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, ListObjectsV2Result, S3ObjectSummary}
import com.amazonaws.services.s3.transfer.TransferManager
import net.kemitix.thorp.core.Resource
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class S3StorageServiceSuite
  extends FunSpec
    with MockFactory {

  describe("listObjectsInPrefix") {
    val source = Resource(this, "upload")
    val sourcePath = source.toPath
    val prefix = RemoteKey("prefix")
    implicit val config: Config = Config(Bucket("bucket"), prefix, sources = Sources(List(sourcePath)))
    implicit val implLogger: Logger = new DummyLogger

    val lm = LastModified(Instant.now.truncatedTo(ChronoUnit.MILLIS))

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
    val storageService = new S3StorageService(amazonS3, amazonS3TransferManager)

    val myFakeResponse = new ListObjectsV2Result()
    val summaries = myFakeResponse.getObjectSummaries
    summaries.add(o1a)
    summaries.add(o1b)
    summaries.add(o2)
    (amazonS3 listObjectsV2 (_: ListObjectsV2Request)).when(*).returns(myFakeResponse)

    it("should build list of hash lookups, with duplicate objects grouped by hash") {
      val expected = Right(S3ObjectsData(
        byHash = Map(
          h1 -> Set(KeyModified(k1a, lm), KeyModified(k1b, lm)),
          h2 -> Set(KeyModified(k2, lm))),
        byKey = Map(
          k1a -> HashModified(h1, lm),
          k1b -> HashModified(h1, lm),
          k2 -> HashModified(h2, lm))))
      val result = storageService.listObjects(Bucket("bucket"), RemoteKey("prefix")).value.unsafeRunSync
      assertResult(expected)(result)
    }
  }

}
