package net.kemitix.thorp.config

import java.nio.file.Paths

import org.scalatest.FunSpec

import scala.jdk.CollectionConverters._

class ParseConfigLinesTest extends FunSpec {

  describe("parse single lines") {
    describe("source") {
      it("should parse") {
        val expected =
          ConfigOptions(
            List[ConfigOption](
              ConfigOption.Source(Paths.get("/path/to/source"))).asJava)
        val result = invoke(List("source = /path/to/source"))
        assertResult(expected)(result)
      }
    }
    describe("bucket") {
      it("should parse") {
        val expected =
          ConfigOptions(
            List[ConfigOption](ConfigOption.Bucket("bucket-name")).asJava)
        val result = invoke(List("bucket = bucket-name"))
        assertResult(expected)(result)
      }
    }
    describe("prefix") {
      it("should parse") {
        val expected =
          ConfigOptions(
            List[ConfigOption](ConfigOption.Prefix("prefix/to/files")).asJava)
        val result = invoke(List("prefix = prefix/to/files"))
        assertResult(expected)(result)
      }
    }
    describe("include") {
      it("should parse") {
        val expected =
          ConfigOptions(
            List[ConfigOption](ConfigOption.Include("path/to/include")).asJava)
        val result = invoke(List("include = path/to/include"))
        assertResult(expected)(result)
      }
    }
    describe("exclude") {
      it("should parse") {
        val expected =
          ConfigOptions(
            List[ConfigOption](ConfigOption.Exclude("path/to/exclude")).asJava)
        val result = invoke(List("exclude = path/to/exclude"))
        assertResult(expected)(result)
      }
    }
    describe("parallel") {
      describe("when valid") {
        it("should parse") {
          val expected =
            ConfigOptions(List[ConfigOption](ConfigOption.Parallel(3)).asJava)
          val result = invoke(List("parallel = 3"))
          assertResult(expected)(result)
        }
      }
      describe("when invalid") {
        it("should ignore") {
          val expected =
            ConfigOptions(List.empty.asJava)
          val result = invoke(List("parallel = invalid"))
          assertResult(expected)(result)
        }
      }
    }
    describe("debug - true") {
      it("should parse") {
        val expected =
          ConfigOptions(List[ConfigOption](ConfigOption.Debug()).asJava)
        val result = invoke(List("debug = true"))
        assertResult(expected)(result)
      }
    }
    describe("debug - false") {
      it("should parse") {
        val expected = ConfigOptions.empty
        val result   = invoke(List("debug = false"))
        assertResult(expected)(result)
      }
    }
    describe("comment line") {
      it("should be ignored") {
        val expected = ConfigOptions.empty
        val result   = invoke(List("# ignore me"))
        assertResult(expected)(result)
      }
    }
    describe("unrecognised option") {
      it("should be ignored") {
        val expected = ConfigOptions.empty
        val result   = invoke(List("unsupported = option"))
        assertResult(expected)(result)
      }
    }

    def invoke(lines: List[String]) = {
      (new ParseConfigLines).parseLines(lines.asJava)
    }
  }
}
