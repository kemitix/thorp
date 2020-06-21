package net.kemitix.thorp.storage.aws

import scala.jdk.CollectionConverters._

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}
import org.scalatest.FunSpec

class S3ObjectsByHashSuite extends FunSpec {

  describe("grouping s3 object together by their hash values") {
    val hash = MD5Hash.create("hash")
    val key1 = RemoteKey.create("key-1")
    val key2 = RemoteKey.create("key-2")
    val o1   = s3object(hash, key1)
    val o2   = s3object(hash, key2)
    val os   = List(o1, o2)
    it("should group by the hash value") {
      val expected: Map[MD5Hash, RemoteKey] = Map(
        hash -> key2
      )
      val result = Map.from(S3Lister.byHash(os.asJava).asScala)
      assertResult(expected)(result)
    }
  }

  private def s3object(md5Hash: MD5Hash,
                       remoteKey: RemoteKey): S3ObjectSummary = {
    val summary = new S3ObjectSummary()
    summary.setETag(md5Hash.hash())
    summary.setKey(remoteKey.key)
    summary
  }

}
