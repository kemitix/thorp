package net.kemitix.thorp.core

import java.io.File

import cats.effect.IO
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, MD5Hash}
import org.scalatest.FunSpec

class LocalFileStreamSuite extends FunSpec {

  val uploadResource = Resource(this, "upload")
  implicit val config: Config = Config(source = uploadResource)
  implicit private val logger: Logger = new DummyLogger
  val md5HashGenerator: File => IO[MD5Hash] = file => MD5HashGenerator.md5File(file)

  describe("findFiles") {
    it("should find all files") {
      val result: Set[String] =
        LocalFileStream.findFiles(uploadResource, md5HashGenerator).unsafeRunSync.toSet
          .map { x: LocalFile => x.relative.toString }
      assertResult(Set("subdir/leaf-file", "root-file"))(result)
    }
  }
}
