package net.kemitix.s3thorp.core

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.domain.{Config, LocalFile, MD5Hash}
import org.scalatest.FunSpec

class LocalFileStreamSuite extends FunSpec {

  val uploadResource = Resource(this, "upload")
  val config: Config = Config(source = uploadResource)
  implicit private val logInfo: Int => String => Unit = l => i => ()
  val md5HashGenerator: File => IO[MD5Hash] = file => MD5HashGenerator.md5File(file)

  describe("findFiles") {
    it("should find all files") {
      val result: Set[String] =
        LocalFileStream.findFiles(uploadResource, md5HashGenerator, logInfo)(config).unsafeRunSync.toSet
          .map { x: LocalFile => x.relative.toString }
      assertResult(Set("subdir/leaf-file", "root-file"))(result)
    }
  }
}
