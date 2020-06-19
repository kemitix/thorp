package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}
import org.scalatest.funspec.AnyFunSpec

class S3ObjectsByHashSuite extends AnyFunSpec {

  describe("grouping s3 object together by their hash values") {
    val hash = MD5Hash("hash")
    val key1 = RemoteKey("key-1")
    val key2 = RemoteKey("key-2")
    val o1   = s3object(hash, key1)
    val o2   = s3object(hash, key2)
    val os   = LazyList(o1, o2)
    it("should group by the hash value") {
      val expected: Map[MD5Hash, RemoteKey] = Map(
        hash -> key2
      )
      val result = Map.from(S3ObjectsByHash.byHash(os))
      assertResult(expected)(result)
    }
  }

  private def s3object(md5Hash: MD5Hash,
                       remoteKey: RemoteKey): S3ObjectSummary = {
    val summary = new S3ObjectSummary()
    summary.setETag(MD5Hash.hash(md5Hash))
    summary.setKey(remoteKey.key)
    summary
  }

}
