package net.kemitix.s3thorp

import java.nio.file.Files

import net.kemitix.s3thorp.domain.{Bucket, Config, MD5Hash, RemoteKey}
import org.scalatest.FunSpec

class MD5HashGeneratorTest extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logInfo: Int => String => Unit = l => i => ()

    describe("read a small file (smaller than buffer)") {
      val file = Resource(this, "upload/root-file")
      it("should generate the correct hash") {
        val expected = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
        val result = MD5HashGenerator.md5File(file)
        assertResult(expected)(result)
      }
    }
    describe("read a buffer") {
      val file = Resource(this, "upload/root-file")
      val buffer: Array[Byte] = Files.readAllBytes(file.toPath)
      it("should generate the correct hash") {
        val expected = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
        val result = MD5HashGenerator.md5PartBody(buffer)
        assertResult(expected)(result)
      }
    }
    describe("read a large file (bigger than buffer)") {
      val file = Resource(this, "big-file")
      it("should generate the correct hash") {
        val expected = MD5Hash("b1ab1f7680138e6db7309200584e35d8")
        val result = MD5HashGenerator.md5File(file)
        assertResult(expected)(result)
      }
    }
    describe("read part of a file") {
      val file = Resource(this, "big-file")
      val halfFileLength = file.length / 2
      assertResult(file.length)(halfFileLength * 2)
      describe("when starting at the beginning of the file") {
        it("should generate the correct hash") {
          val expected = MD5Hash("aadf0d266cefe0fcdb241a51798d74b3")
          val result = MD5HashGenerator.md5FilePart(file, 0, halfFileLength)
          assertResult(expected)(result)
        }
      }
      describe("when starting in the middle of the file") {
        it("should generate the correct hash") {
          val expected = MD5Hash("16e08d53ca36e729d808fd5e4f7e35dc")
          val result = MD5HashGenerator.md5FilePart(file, halfFileLength, halfFileLength)
          assertResult(expected)(result)
        }
      }
    }

}
