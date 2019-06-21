package net.kemitix.thorp.aws.lib

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{KeyModified, LastModified, MD5Hash, RemoteKey}
import org.scalatest.FunSpec

class S3ObjectsByHashSuite extends FunSpec {

    describe("grouping s3 object together by their hash values") {
      val hash = MD5Hash("hash")
      val key1 = RemoteKey("key-1")
      val key2 = RemoteKey("key-2")
      val lastModified = LastModified(Instant.now.truncatedTo(ChronoUnit.MILLIS))
      val o1 = s3object(hash, key1, lastModified)
      val o2 = s3object(hash, key2, lastModified)
      val os = Stream(o1, o2)
      it("should group by the hash value") {
        val expected: Map[MD5Hash, Set[KeyModified]] = Map(
          hash -> Set(KeyModified(key1, lastModified), KeyModified(key2, lastModified))
        )
        val result = S3ObjectsByHash.byHash(os)
        assertResult(expected)(result)
      }
    }

  private def s3object(md5Hash: MD5Hash,
                       remoteKey: RemoteKey,
                       lastModified: LastModified): S3ObjectSummary = {
    val summary = new S3ObjectSummary()
    summary.setETag(md5Hash.hash)
    summary.setKey(remoteKey.key)
    summary.setLastModified(Date.from(lastModified.when))
    summary
  }

}
