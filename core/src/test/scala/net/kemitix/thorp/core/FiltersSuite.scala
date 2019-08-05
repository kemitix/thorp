package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.domain.Filter
import net.kemitix.thorp.domain.Filter.{Exclude, Include}
import org.scalatest.FunSpec

class FiltersSuite extends FunSpec {

  private val path1 = "a-file"
  private val path2 = "another-file.txt"
  private val path3 = "/path/to/a/file.txt"
  private val path4 = "/path/to/another/file"
  private val path5 = "/home/pcampbell/repos/kemitix/s3thorp"
  private val path6 = "/kemitix/s3thorp/upload/subdir"
  private val paths =
    List(path1, path2, path3, path4, path5, path6).map(Paths.get(_))

  describe("Include") {

    describe("default filter") {
      val include = Include()
      it("should include files") {
        paths.foreach(path =>
          assertResult(true)(Filters.isIncludedByFilter(path)(include)))
      }
    }
    describe("directory exact match include '/upload/subdir/'") {
      val include = Include("/upload/subdir/")
      it("include matching directory") {
        val matching = Paths.get("/upload/subdir/leaf-file")
        assertResult(true)(Filters.isIncludedByFilter(matching)(include))
      }
      it("exclude non-matching files") {
        val nonMatching = Paths.get("/upload/other-file")
        assertResult(false)(Filters.isIncludedByFilter(nonMatching)(include))
      }
    }
    describe("file partial match 'root'") {
      val include = Include("root")
      it("include matching file '/upload/root-file") {
        val matching = Paths.get("/upload/root-file")
        assertResult(true)(Filters.isIncludedByFilter(matching)(include))
      }
      it("exclude non-matching files 'test-file-for-hash.txt' & '/upload/subdir/leaf-file'") {
        val nonMatching1 = Paths.get("/test-file-for-hash.txt")
        val nonMatching2 = Paths.get("/upload/subdir/leaf-file")
        assertResult(false)(Filters.isIncludedByFilter(nonMatching1)(include))
        assertResult(false)(Filters.isIncludedByFilter(nonMatching2)(include))
      }
    }
  }

  describe("Exclude") {
//    describe("default exclude") {
//      val exclude = Exclude()
//      it("should exclude all files") {
//        paths.foreach(path => {
//          assertResult(true)(exclude.isExcluded(path))
//        })
//      }
//    }
    describe("directory exact match exclude '/upload/subdir/'") {
      val exclude = Exclude("/upload/subdir/")
      it("exclude matching directory") {
        val matching = Paths.get("/upload/subdir/leaf-file")
        assertResult(true)(Filters.isExcludedByFilter(matching)(exclude))
      }
      it("include non-matching files") {
        val nonMatching = Paths.get("/upload/other-file")
        assertResult(false)(Filters.isExcludedByFilter(nonMatching)(exclude))
      }
    }
    describe("file partial match 'root'") {
      val exclude = Exclude("root")
      it("exclude matching file '/upload/root-file") {
        val matching = Paths.get("/upload/root-file")
        assertResult(true)(Filters.isExcludedByFilter(matching)(exclude))
      }
      it("include non-matching files 'test-file-for-hash.txt' & '/upload/subdir/leaf-file'") {
        val nonMatching1 = Paths.get("/test-file-for-hash.txt")
        val nonMatching2 = Paths.get("/upload/subdir/leaf-file")
        assertResult(false)(Filters.isExcludedByFilter(nonMatching1)(exclude))
        assertResult(false)(Filters.isExcludedByFilter(nonMatching2)(exclude))
      }
    }
  }
  describe("isIncluded") {
    def invoke(filters: List[Filter]) = {
      paths.filter(path => Filters.isIncluded(path)(filters))
    }

    describe("when there are no filters") {
      val filters = List[Filter]()
      it("should accept all files") {
        val expected = paths
        val result   = invoke(filters)
        assertResult(expected)(result)
      }
    }
    describe("when a single include") {
      val filters = List(Include(".txt"))
      it("should only include two matching paths") {
        val expected = List(path2, path3).map(Paths.get(_))
        val result   = invoke(filters)
        assertResult(expected)(result)
      }
    }
    describe("when a single exclude") {
      val filters = List(Exclude("path"))
      it("should include only other paths") {
        val expected = List(path1, path2, path5, path6).map(Paths.get(_))
        val result   = invoke(filters)
        assertResult(expected)(result)
      }
    }
    describe("when include .txt files, but then exclude everything trumps all") {
      val filters = List(Include(".txt"), Exclude(".*"))
      it("should include nothing") {
        val expected = List()
        val result   = invoke(filters)
        assertResult(expected)(result)
      }
    }
    describe("when exclude everything except .txt files") {
      val filters = List(Exclude(".*"), Include(".txt"))
      it("should include only the .txt files") {
        val expected = List(path2, path3).map(Paths.get(_))
        val result   = invoke(filters)
        assertResult(expected)(result)
      }
    }
  }
}
