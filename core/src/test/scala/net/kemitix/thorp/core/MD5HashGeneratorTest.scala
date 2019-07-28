package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.config.Resource
import net.kemitix.thorp.domain.MD5HashData.{BigFile, Root}
import org.scalatest.FunSpec
import zio.DefaultRuntime

class MD5HashGeneratorTest extends FunSpec {

  private val runtime = new DefaultRuntime {}

  private val source = Resource(this, "upload")

  describe("md5File()") {
    describe("read a small file (smaller than buffer)") {
      val path = Resource(this, "upload/root-file").toPath
      it("should generate the correct hash") {
        val expected = Right(Root.hash)
        val result   = invoke(path)
        assertResult(expected)(result)
      }
    }

    describe("read a large file (bigger than buffer)") {
      val path = Resource(this, "big-file").toPath
      it("should generate the correct hash") {
        val expected = Right(BigFile.hash)
        val result   = invoke(path)
        assertResult(expected)(result)
      }
    }

    def invoke(path: Path) = {
      runtime.unsafeRunSync {
        MD5HashGenerator.md5File(path)
      }.toEither
    }
  }

  describe("md5FileChunk") {
    describe("read chunks of file") {
      val path = Resource(this, "big-file").toPath
      it("should generate the correct hash for first chunk of the file") {
        val part1    = BigFile.Part1
        val expected = Right(part1.hash.hash)
        val result   = invoke(path, part1.offset, part1.size).map(_.hash)
        assertResult(expected)(result)
      }
      it("should generate the correct hash for second chunk of the file") {
        val part2    = BigFile.Part2
        val expected = Right(part2.hash.hash)
        val result   = invoke(path, part2.offset, part2.size).map(_.hash)
        assertResult(expected)(result)
      }
    }

    def invoke(path: Path, offset: Long, size: Long) = {
      runtime.unsafeRunSync {
        MD5HashGenerator.md5FileChunk(path, offset, size)
      }.toEither
    }
  }

}
