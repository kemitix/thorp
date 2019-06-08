package net.kemitix.s3thorp.domain

import java.nio.file.{Path, Paths}

import org.scalatest.FunSpec

class IncludeSuite extends FunSpec {

  describe("default filter") {
    val include = Include()
    val paths: List[Path] = List("/a-file", "a-file", "path/to/a/file", "/path/to/a/file",
      "/home/pcampbell/repos/kemitix/s3thorp/target/scala-2.12/test-classes/net/kemitix/s3thorp/upload/subdir"
    ) map { p => Paths.get(p)}
    it("should not exclude files") {
      paths.foreach(path => { assertResult(false)(include.isExcluded(path)) })
    }
    it("should include files") {
      paths.foreach(path => assertResult(true)(include.isIncluded(path)))
    }
  }
  describe("directory exact match include '/upload/subdir/'") {
    val include = Include("/upload/subdir/")
    it("include matching directory") {
      val matching = Paths.get("/upload/subdir/leaf-file")
      assertResult(true)(include.isIncluded(matching))
    }
    it("exclude non-matching files") {
      val nonMatching = Paths.get("/upload/other-file")
      assertResult(true)(include.isExcluded(nonMatching))
    }
  }
  describe("file partial match 'root'") {
    val include = Include("root")
    it("include matching file '/upload/root-file") {
      val matching = Paths.get("/upload/root-file")
      assertResult(true)(include.isIncluded(matching))
    }
    it("exclude non-matching files 'test-file-for-hash.txt' & '/upload/subdir/leaf-file'") {
      val nonMatching1 = Paths.get("/test-file-for-hash.txt")
      val nonMatching2 = Paths.get("/upload/subdir/leaf-file")
      assertResult(true)(include.isExcluded(nonMatching1))
      assertResult(true)(include.isExcluded(nonMatching2))
    }
  }

}
