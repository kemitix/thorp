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
  describe("read chunks of file") {
    val file = Resource(this, "big-file")
    it("should generate the correct hash for first chunk of the file") {
      val part1 = MD5HashData.BigFile.Part1
      val expected = part1.hash
      val result = MD5HashGenerator.md5FileChunk(file, part1.offset, part1.size).unsafeRunSync
      assertResult(expected)(result)
    }
    it("should generate the correcy hash for second chunk of the file") {
      val part2 = MD5HashData.BigFile.Part2
      val expected = part2.hash
      val result = MD5HashGenerator.md5FileChunk(file, part2.offset, part2.size).unsafeRunSync
      assertResult(expected)(result)
    }
  }

}
