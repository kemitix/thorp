package net.kemitix.thorp.config

import java.io.File
import java.nio.file.Paths
import java.util

import net.kemitix.thorp.filesystem.TemporaryFolder
import org.scalatest.FunSpec
import zio.DefaultRuntime

import scala.jdk.CollectionConverters._

class ParseConfigFileTest extends FunSpec with TemporaryFolder {

  private val empty = Right(ConfigOptions.empty)

  describe("parse a missing file") {
    val file = new File("/path/to/missing/file")
    it("should return no options") {
      assertResult(empty)(invoke(file))
    }
  }
  describe("parse an empty file") {
    it("should return no options") {
      withDirectory(dir => {
        val file = createFile(dir, "empty-file", List.empty[String].asJava)
        assertResult(empty)(invoke(file))
      })
    }
  }
  describe("parse a file with no valid entries") {
    it("should return no options") {
      withDirectory(dir => {
        val file = createFile(dir,
                              "invalid-config",
                              util.Arrays.asList("no valid = config items"))
        assertResult(empty)(invoke(file))
      })
    }
  }
  describe("parse a file with properties") {
    it("should return some options") {
      val expected = Right(
        ConfigOptions(
          List[ConfigOption](ConfigOption.Source(Paths.get("/path/to/source")),
                             ConfigOption.Bucket("bucket-name")).asJava))
      withDirectory(dir => {
        val file = createFile(dir,
                              "simple-config",
                              util.Arrays.asList("source = /path/to/source",
                                                 "bucket = bucket-name"))
        assertResult(expected)(invoke(file))
      })
    }
  }

  private def invoke(file: File) = {
    new DefaultRuntime {}.unsafeRunSync {
      ParseConfigFile.parseFile(file)
    }.toEither
  }
}
