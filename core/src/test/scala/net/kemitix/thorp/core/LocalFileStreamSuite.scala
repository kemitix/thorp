package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.domain.{Config, LocalFile, Logger, MD5HashData}
import net.kemitix.thorp.storage.api.HashService
import org.scalatest.FunSpec

class LocalFileStreamSuite extends FunSpec {

  private val uploadResource = Resource(this, "upload")
  private val hashService: HashService = DummyHashService(Map(
    file("root-file") -> Map("md5" -> MD5HashData.Root.hash),
    file("subdir/leaf-file") -> Map("md5" -> MD5HashData.Leaf.hash)
  ))

  private def file(filename: String) =
    uploadResource.toPath.resolve(Paths.get(filename)).toFile

  implicit private val config: Config = Config(source = uploadResource)
  implicit private val logger: Logger = new DummyLogger

  describe("findFiles") {
    it("should find all files") {
      val result: Set[String] =
        invoke.localFiles.toSet
          .map { x: LocalFile => x.relative.toString }
      assertResult(Set("subdir/leaf-file", "root-file"))(result)
    }
  }

  private def invoke = {
    LocalFileStream.findFiles(uploadResource, hashService).unsafeRunSync
  }
}
