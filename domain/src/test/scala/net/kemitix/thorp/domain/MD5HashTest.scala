package net.kemitix.thorp.domain

import org.scalatest.funspec.AnyFunSpec

class MD5HashTest extends AnyFunSpec {

  describe("recover base64 hash") {
    it("should recover base 64 #1") {
      val rootHash = MD5HashData.Root.hash
      assertResult(MD5HashData.Root.base64)(MD5Hash.hash64(rootHash))
    }
    it("should recover base 64 #2") {
      val leafHash = MD5HashData.Leaf.hash
      assertResult(MD5HashData.Leaf.base64)(MD5Hash.hash64(leafHash))
    }
  }
}
