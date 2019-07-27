package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.HashService
import org.scalatest.FunSpec
import zio.DefaultRuntime

class LocalFileStreamSuite extends FunSpec {

  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val hashService: HashService = DummyHashService(
    Map(
      file("root-file")        -> Map(MD5 -> MD5HashData.Root.hash),
      file("subdir/leaf-file") -> Map(MD5 -> MD5HashData.Leaf.hash)
    ))

  private def file(filename: String) =
    sourcePath.resolve(Paths.get(filename))

  implicit private val config: LegacyConfig = LegacyConfig(
    sources = Sources(List(sourcePath)))

  describe("findFiles") {
    it("should find all files") {
      val expected = Right(Set("subdir/leaf-file", "root-file"))
      val result =
        invoke()
          .map(_.localFiles)
          .map(localFiles => localFiles.map(_.relative.toString))
          .map(_.toSet)
      assertResult(expected)(result)
    }
    it("should count all files") {
      val expected = Right(2)
      val result   = invoke().map(_.count)
      assertResult(expected)(result)
    }
    it("should sum the size of all files") {
      val expected = Right(113)
      val result   = invoke().map(_.totalSizeBytes)
      assertResult(expected)(result)
    }
  }

  private def invoke() = {
    val runtime = new DefaultRuntime {}
    runtime.unsafeRunSync {
      LocalFileStream.findFiles(sourcePath, hashService)
    }.toEither
  }

}
