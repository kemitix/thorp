package net.kemitix.s3thorp

import java.nio.file.{Path, Paths}

class FilterSuite extends UnitTest {

  describe("default filter") {
    val filter = Filter()
    val paths: List[Path] = List("/a-file", "a-file", "path/to/a/file", "/path/to/a/file",
      "/home/pcampbell/repos/kemitix/s3thorp/target/scala-2.12/test-classes/net/kemitix/s3thorp/upload/subdir"
    ) map { p => Paths.get(p)}
    it("should not exclude files") {
      paths.foreach(path => { assertResult(false)(filter.isExcluded(path)) })
    }
    it("should include files") {
      paths.foreach(path => assertResult(true)(filter.isIncluded(path)))
    }
  }
  describe("directory exact match include '/upload/subdir/'") {
    val filter = Filter("/upload/subdir/")
    it("include matching directory") {
      val matching = Paths.get("/upload/subdir/leaf-file")
      assertResult(true)(filter.isIncluded(matching))
    }
    it("exclude non-matching files") {
      val nonMatching = Paths.get("/upload/other-file")
      assertResult(true)(filter.isExcluded(nonMatching))
    }
  }
  describe("file partial match 'root'") {
    val filter = Filter("root")
    it("include matching file '/upload/root-file") {
      val matching = Paths.get("/upload/root-file")
      assertResult(true)(filter.isIncluded(matching))
    }
    it("exclude non-matching files 'test-file-for-hash.txt' & '/upload/subdir/leaf-file'") {
      val nonMatching1 = Paths.get("/test-file-for-hash.txt")
      val nonMatching2 = Paths.get("/upload/subdir/leaf-file")
      assertResult(true)(filter.isExcluded(nonMatching1))
      assertResult(true)(filter.isExcluded(nonMatching2))
    }
  }

}
