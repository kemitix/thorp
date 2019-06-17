package net.kemitix.thorp.core

import cats.Id
import net.kemitix.thorp.core.MD5HashData.rootHash
import net.kemitix.thorp.domain._
import org.scalatest.FunSpec

class MD5HashGeneratorTest extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logger: Logger[Id] = new DummyLogger[Id]

    describe("read a small file (smaller than buffer)") {
      val file = Resource(this, "upload/root-file")
      it("should generate the correct hash") {
        val result = MD5HashGenerator.md5File[Id](file)
        assertResult(rootHash)(result)
      }
    }
    describe("read a large file (bigger than buffer)") {
      val file = Resource(this, "big-file")
      it("should generate the correct hash") {
        val expected = MD5Hash("b1ab1f7680138e6db7309200584e35d8")
        val result = MD5HashGenerator.md5File[Id](file)
        assertResult(expected)(result)
      }
    }

}
