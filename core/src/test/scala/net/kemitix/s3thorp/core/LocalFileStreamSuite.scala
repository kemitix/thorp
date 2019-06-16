package net.kemitix.s3thorp.core

import java.io.File

import cats.Id
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, MD5Hash}
import org.scalatest.FunSpec

class LocalFileStreamSuite extends FunSpec {

  val uploadResource = Resource(this, "upload")
  implicit val config: Config = Config(source = uploadResource)
  implicit private val logger: Logger[Id] = new DummyLogger[Id]
  val md5HashGenerator: File => Id[MD5Hash] = file => MD5HashGenerator.md5File[Id](file)

  describe("findFiles") {
    it("should find all files") {
      val result: Set[String] =
        LocalFileStream.findFiles[Id](uploadResource, md5HashGenerator).toSet
          .map { x: LocalFile => x.relative.toString }
      assertResult(Set("subdir/leaf-file", "root-file"))(result)
    }
  }
}
