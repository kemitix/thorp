package net.kemitix.s3thorp

import java.io.File

import org.scalatest.FunSpec

class LocalFileStreamSuite extends FunSpec with LocalFileStream {

  describe("findFiles") {
    val uploadResource = Resource(this, "upload")
    val config: Config = Config(source = uploadResource)
    it("should find all files") {
      val result: Set[String] = findFiles(uploadResource)(config).toSet
          .map { x: File => uploadResource.toPath.relativize(x.toPath).toString }
      assertResult(Set("subdir/leaf-file", "root-file"))(result)
    }
  }
}
