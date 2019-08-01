package net.kemitix.thorp.core.hasher

import java.nio.file.Path

import net.kemitix.thorp.config.Resource
import net.kemitix.thorp.domain.MD5HashData.{BigFile, Root}
import net.kemitix.thorp.filesystem.FileSystem
import org.scalatest.FunSpec
import zio.DefaultRuntime

class MD5HashGeneratorTest extends FunSpec {

  describe("md5File()") {
    describe("read a small file (smaller than buffer)") {
      val path = Resource(this, "../upload/root-file").toPath
      it("should generate the correct hash") {
        val expected = Right(Root.hash)
        val result   = invoke(path)
        assertResult(expected)(result)
      }
    }

    describe("read a large file (bigger than buffer)") {
      val path = Resource(this, "../big-file").toPath
      it("should generate the correct hash") {
        val expected = Right(BigFile.hash)
        val result   = invoke(path)
        assertResult(expected)(result)
      }
    }

    def invoke(path: Path) =
      new DefaultRuntime {}.unsafeRunSync {
        MD5HashGenerator
          .md5File(path)
          .provide(testEnv)
      }.toEither
  }

  describe("md5FileChunk") {
    describe("read chunks of file") {
      val path = Resource(this, "../big-file").toPath
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

    def invoke(path: Path, offset: Long, size: Long) =
      new DefaultRuntime {}.unsafeRunSync {
        MD5HashGenerator
          .md5FileChunk(path, offset, size)
          .provide(testEnv)
      }.toEither
  }

  type TestEnv = FileSystem
  val testEnv: TestEnv = new FileSystem.Live {}

}
