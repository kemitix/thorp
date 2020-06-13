package net.kemitix.thorp.filesystem

import java.nio.file.Path

import net.kemitix.thorp.domain.MD5HashData.{BigFile, Root}
import org.scalatest.FunSpec
import zio.DefaultRuntime

class MD5HashGeneratorTest extends FunSpec {

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

    def invoke(path: Path) =
      new DefaultRuntime {}.unsafeRunSync {
        MD5HashGenerator
          .md5File(path)
          .provide(testEnv)
      }.toEither
  }

  describe("md5FileChunk") {
    describe("read chunks of file") {
      val path = Resource(this, "big-file").toPath
      it("should generate the correct hash for first chunk of the file") {
        val expected = Right(BigFile.Part1.hash.hash())
        val result =
          invoke(path, BigFile.Part1.offset, BigFile.Part1.size).map(x =>
            x.hash)
        assertResult(expected)(result)
      }
      it("should generate the correct hash for second chunk of the file") {
        val expected = Right(BigFile.Part2.hash.hash())
        val result =
          invoke(path, BigFile.Part2.offset, BigFile.Part2.size).map(x =>
            x.hash)
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
