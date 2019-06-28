package net.kemitix.thorp.core

import net.kemitix.thorp.domain.MD5HashData.Root
import net.kemitix.thorp.domain._
import org.scalatest.FunSpec

class MD5HashGeneratorTest extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logger: Logger = new DummyLogger

    describe("read a small file (smaller than buffer)") {
      val file = Resource(this, "upload/root-file")
      it("should generate the correct hash") {
        val result = MD5HashGenerator.md5File(file).unsafeRunSync
        assertResult(Root.hash)(result)
      }
    }
    describe("read a large file (bigger than buffer)") {
      val file = Resource(this, "big-file")
      it("should generate the correct hash") {
        val expected = MD5HashData.BigFile.hash
        val result = MD5HashGenerator.md5File(file).unsafeRunSync
        assertResult(expected)(result)
      }
    }

}
