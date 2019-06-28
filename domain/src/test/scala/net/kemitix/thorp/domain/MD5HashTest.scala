package net.kemitix.thorp.domain

import org.scalatest.FunSpec

class MD5HashTest extends FunSpec {

  describe("recover base64 hash") {
    it("should recover base 64 #1") {
      val rootHash = MD5HashData.rootHash
      assertResult(MD5HashData.rootBase64Hash)(rootHash.hash64)
    }
    it("should recover base 64 #2") {
      val leafHash = MD5HashData.leafHash
      assertResult(MD5HashData.leafBase64Hash)(leafHash.hash64)
    }
  }
}
