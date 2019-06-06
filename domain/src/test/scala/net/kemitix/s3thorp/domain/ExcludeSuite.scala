package net.kemitix.s3thorp.domain

import java.nio.file.{Path, Paths}

import org.scalatest.FunSpec

class ExcludeSuite extends FunSpec {

  describe("default exclude") {
    val exclude = Exclude()
    val paths: List[Path] = List("/a-file", "a-file", "path/to/a/file", "/path/to/a/file",
      "/home/pcampbell/repos/kemitix/s3thorp/target/scala-2.12/test-classes/net/kemitix/s3thorp/upload/subdir"
    ) map { p => Paths.get(p)}
    it("should not exclude files") {
      paths.foreach(path => { assertResult(false)(exclude.isExcluded(path)) })
    }
    it("should include files") {
      paths.foreach(path => assertResult(true)(exclude.isIncluded(path)))
    }
  }
  describe("directory exact match exclude '/upload/subdir/'") {
    val exclude = Exclude("/upload/subdir/")
    it("exclude matching directory") {
      val matching = Paths.get("/upload/subdir/leaf-file")
      assertResult(true)(exclude.isExcluded(matching))
    }
    it("include non-matching files") {
      val nonMatching = Paths.get("/upload/other-file")
      assertResult(true)(exclude.isIncluded(nonMatching))
    }
  }
  describe("file partial match 'root'") {
    val exclude = Exclude("root")
    it("exclude matching file '/upload/root-file") {
      val matching = Paths.get("/upload/root-file")
      assertResult(true)(exclude.isExcluded(matching))
    }
    it("include non-matching files 'test-file-for-hash.txt' & '/upload/subdir/leaf-file'") {
      val nonMatching1 = Paths.get("/test-file-for-hash.txt")
      val nonMatching2 = Paths.get("/upload/subdir/leaf-file")
      assertResult(true)(exclude.isIncluded(nonMatching1))
      assertResult(true)(exclude.isIncluded(nonMatching2))
    }
  }

}
