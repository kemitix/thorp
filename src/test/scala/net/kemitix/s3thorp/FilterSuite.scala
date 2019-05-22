package net.kemitix.s3thorp

import java.nio.file.Paths

class FilterSuite extends UnitTest {

  describe("default filter") {
    val filter = Filter()
    val path = Paths.get("/a-file")
    it("should not exclude files") {
      assert(!filter.isExcluded(path))
    }
    it("should include files") {
      assert(filter.isIncluded(path))
    }
  }
  describe("directory exact match filter '/upload/subdir/'") {
    val filter = Filter("/upload/subdir/")
    it("exclude matching directory") {
      val matching = Paths.get("/upload/subdir/leaf-file")
      assert(filter.isExcluded(matching))
    }
    it("include non-matching files") {
      val nonMatching = Paths.get("/upload/other-file")
      assert(filter.isIncluded(nonMatching))
    }
  }
  describe("file partial match 'root'") {
    val filter = Filter("root")
    it("exclude matching file '/upload/root-file") {
      val matching = Paths.get("/upload/root-file")
      assert(filter.isExcluded(matching))
    }
    it("include non-matching files 'test-file-for-hash.txt' & '/upload/subdir/leaf-file'") {
      val nonMatching1 = Paths.get("/test-file-for-hash.txt")
      val nonMatching2 = Paths.get("/upload/subdir/leaf-file")
      assert(filter.isIncluded(nonMatching1))
      assert(filter.isIncluded(nonMatching2))
    }
  }

}
