package net.kemitix.thorp.core

import java.nio.file.Paths

import org.scalatest.FunSpec

class ParseConfigLinesTest extends FunSpec {

  describe("parse single lines") {
    describe("source") {
      it("should parse") {
        val expected = List(ConfigOption.Source(Paths.get("/path/to/source")))
        val result = ParseConfigLines(List("source = /path/to/source"))
        assertResult(expected)(result)
      }
    }
    describe("bucket") {
      it("should parse") {
        val expected = List(ConfigOption.Bucket("bucket-name"))
        val result = ParseConfigLines(List("bucket = bucket-name"))
        assertResult(expected)(result)
      }
    }
    describe("prefix") {
      it("should parse") {
        val expected = List(ConfigOption.Prefix("prefix/to/files"))
        val result = ParseConfigLines(List("prefix = prefix/to/files"))
        assertResult(expected)(result)
      }
    }
    describe("include") {
      it("should parse") {
        val expected = List(ConfigOption.Include("path/to/include"))
        val result = ParseConfigLines(List("include = path/to/include"))
        assertResult(expected)(result)
      }
    }
    describe("exclude") {
      it("should parse") {
        val expected = List(ConfigOption.Exclude("path/to/exclude"))
        val result = ParseConfigLines(List("exclude = path/to/exclude"))
        assertResult(expected)(result)
      }
    }
    describe("debug - true") {
      it("should parse") {
        val expected = List(ConfigOption.Debug())
        val result = ParseConfigLines(List("debug = true"))
        assertResult(expected)(result)
      }
    }
    describe("debug - false") {
      it("should parse") {
        val expected = List()
        val result = ParseConfigLines(List("debug = false"))
        assertResult(expected)(result)
      }
    }
    describe("comment line") {
      it("should be ignored") {
        val expected = List()
        val result = ParseConfigLines(List("# ignore me"))
        assertResult(expected)(result)
      }
    }
    describe("unrecognised option") {
      it("should be ignored") {
        val expected = List()
        val result = ParseConfigLines(List("unsupported = option"))
        assertResult(expected)(result)
      }
    }
  }
}
