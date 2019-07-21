package net.kemitix.thorp.core

import java.nio.file.{Path, Paths}

import org.scalatest.FunSpec
import zio.DefaultRuntime

class ParseConfigFileTest extends FunSpec {

  private val empty = Right(ConfigOptions())

  describe("parse a missing file") {
    val filename = Paths.get("/path/to/missing/file")
    it("should return no options") {
      assertResult(empty)(invoke(filename))
    }
  }
  describe("parse an empty file") {
    val filename = Resource(this, "empty-file").toPath
    it("should return no options") {
      assertResult(empty)(invoke(filename))
    }
  }
  describe("parse a file with no valid entries") {
    val filename = Resource(this, "invalid-config").toPath
    it("should return no options") {
      assertResult(empty)(invoke(filename))
    }
  }
  describe("parse a file with properties") {
    val filename = Resource(this, "simple-config").toPath
    val expected = Right(
      ConfigOptions(List(ConfigOption.Source(Paths.get("/path/to/source")),
                         ConfigOption.Bucket("bucket-name"))))
    it("should return some options") {
      assertResult(expected)(invoke(filename))
    }
  }

  private def invoke(filename: Path) = {
    val runtime = new DefaultRuntime {}
    runtime.unsafeRunSync {
      ParseConfigFile.parseFile(filename)
    }.toEither
  }
}
