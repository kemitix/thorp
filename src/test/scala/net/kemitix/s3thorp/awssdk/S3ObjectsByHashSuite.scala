package net.kemitix.s3thorp.awssdk

import java.time.Instant

import net.kemitix.s3thorp.domain.RemoteKey
import net.kemitix.s3thorp.{KeyModified, LastModified, MD5Hash, UnitTest}
import software.amazon.awssdk.services.s3.model.S3Object

class S3ObjectsByHashSuite extends UnitTest {

  new S3ObjectsByHash {
    describe("grouping s3 object together by their hash values") {
      val hash = MD5Hash("hash")
      val key1 = RemoteKey("key-1")
      val key2 = RemoteKey("key-2")
      val lastModified = LastModified(Instant.now)
      val o1 = s3object(hash, key1, lastModified)
      val o2 = s3object(hash, key2, lastModified)
      val os = Stream(o1, o2)
      it("should group by the hash value") {
        val expected: Map[MD5Hash, Set[KeyModified]] = Map(
          hash -> Set(KeyModified(key1, lastModified), KeyModified(key2, lastModified))
        )
        val result = byHash(os)
        assertResult(expected)(result)
      }
    }
  }

  private def s3object(md5Hash: MD5Hash, remoteKey: RemoteKey, lastModified: LastModified): S3Object =
    S3Object.builder
      .eTag(md5Hash.hash)
      .key(remoteKey.key)
      .lastModified(lastModified.when)
      .build

}
