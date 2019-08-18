package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.domain.{RemoteKey, Sources}
import net.kemitix.thorp.filesystem.Resource
import org.scalatest.FunSpec
import zio.DefaultRuntime

class KeyGeneratorSuite extends FunSpec {

  private val source: File = Resource(this, "upload")
  private val sourcePath   = source.toPath
  private val prefix       = RemoteKey("prefix")
  private val sources      = Sources(List(sourcePath))

  describe("key generator") {

    describe("when file is within source") {
      it("has a valid key") {
        val subdir   = "subdir"
        val expected = Right(RemoteKey(s"${prefix.key}/$subdir"))
        val result   = invoke(sourcePath.resolve(subdir))
        assertResult(expected)(result)
      }
    }

    describe("when file is deeper within source") {
      it("has a valid key") {
        val subdir   = "subdir/deeper/still"
        val expected = Right(RemoteKey(s"${prefix.key}/$subdir"))
        val result   = invoke(sourcePath.resolve(subdir))
        assertResult(expected)(result)
      }
    }

    def invoke(path: Path) = {
      new DefaultRuntime {}.unsafeRunSync {
        KeyGenerator.generateKey(sources, prefix)(path)
      }.toEither
    }
  }

}
