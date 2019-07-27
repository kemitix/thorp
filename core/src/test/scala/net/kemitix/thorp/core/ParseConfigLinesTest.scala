package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.config.{ConfigOption, ConfigOptions, ParseConfigLines}
import org.scalatest.FunSpec

class ParseConfigLinesTest extends FunSpec {

  describe("parse single lines") {
    describe("source") {
      it("should parse") {
        val expected =
          ConfigOptions(List(ConfigOption.Source(Paths.get("/path/to/source"))))
        val result =
          ParseConfigLines.parseLines(List("source = /path/to/source"))
        assertResult(expected)(result)
      }
    }
    describe("bucket") {
      it("should parse") {
        val expected = ConfigOptions(List(ConfigOption.Bucket("bucket-name")))
        val result   = ParseConfigLines.parseLines(List("bucket = bucket-name"))
        assertResult(expected)(result)
      }
    }
    describe("prefix") {
      it("should parse") {
        val expected =
          ConfigOptions(List(ConfigOption.Prefix("prefix/to/files")))
        val result =
          ParseConfigLines.parseLines(List("prefix = prefix/to/files"))
        assertResult(expected)(result)
      }
    }
    describe("include") {
      it("should parse") {
        val expected =
          ConfigOptions(List(ConfigOption.Include("path/to/include")))
        val result =
          ParseConfigLines.parseLines(List("include = path/to/include"))
        assertResult(expected)(result)
      }
    }
    describe("exclude") {
      it("should parse") {
        val expected =
          ConfigOptions(List(ConfigOption.Exclude("path/to/exclude")))
        val result =
          ParseConfigLines.parseLines(List("exclude = path/to/exclude"))
        assertResult(expected)(result)
      }
    }
    describe("debug - true") {
      it("should parse") {
        val expected = ConfigOptions(List(ConfigOption.Debug()))
        val result   = ParseConfigLines.parseLines(List("debug = true"))
        assertResult(expected)(result)
      }
    }
    describe("debug - false") {
      it("should parse") {
        val expected = ConfigOptions()
        val result   = ParseConfigLines.parseLines(List("debug = false"))
        assertResult(expected)(result)
      }
    }
    describe("comment line") {
      it("should be ignored") {
        val expected = ConfigOptions()
        val result   = ParseConfigLines.parseLines(List("# ignore me"))
        assertResult(expected)(result)
      }
    }
    describe("unrecognised option") {
      it("should be ignored") {
        val expected = ConfigOptions()
        val result   = ParseConfigLines.parseLines(List("unsupported = option"))
        assertResult(expected)(result)
      }
    }
  }
}
