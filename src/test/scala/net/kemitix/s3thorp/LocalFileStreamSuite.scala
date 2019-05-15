package net.kemitix.s3thorp

import org.scalatest.FunSpec

class LocalFileStreamSuite extends FunSpec with LocalFileStream {

  describe("streamDirectoryPaths") {
    var uploadResource = Resource(this, "upload")
    it("should find all files") {
      val result: List[String] = streamDirectoryPaths(uploadResource).toList
          .map(x=>uploadResource.toPath.relativize(x.toPath).toString)
      assertResult(List("subdir/leaf-file", "root-file"))(result)
    }
  }
}
