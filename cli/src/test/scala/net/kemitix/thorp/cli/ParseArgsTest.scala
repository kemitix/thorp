package net.kemitix.thorp.cli

import java.nio.file.Paths

import net.kemitix.thorp.core.ConfigOption.Debug
import net.kemitix.thorp.core.{ConfigOptions, ConfigQuery, Resource}
import org.scalatest.FunSpec

import scala.util.Try

class ParseArgsTest extends FunSpec {

  val source = Resource(this, "")

  describe("parse - source") {
    def invokeWithSource(path: String) =
      ParseArgs(List("--source", path, "--bucket", "bucket"))

    describe("when source is a directory") {
      it("should succeed") {
        val result = invokeWithSource(pathTo("."))
        assert(result.isDefined)
      }
    }
    describe("when source is a relative path to a directory") {
      val result = invokeWithSource(pathTo("."))
      it("should succeed") {pending}
    }
    describe("when there are multiple sources") {
      val maybeConfigOptions = ParseArgs(List(
        "--source", "path1",
        "--source", "path2",
        "--bucket", "bucket"))
      it("should accept more than one source") {
        assert(maybeConfigOptions.isDefined)
      }
      it("should get multiple sources") {
        val expected = Set("path1", "path2").map(Paths.get(_))
        val configOptions = maybeConfigOptions.get
        val result = ConfigQuery.sources(configOptions).toSet
        assertResult(expected)(result)
      }
    }
  }

  describe("parse - debug") {
    def invokeWithArgument(arg: String): ConfigOptions = {
      val strings = List("--source", pathTo("."), "--bucket", "bucket", arg)
          .filter(_ != "")
      val maybeOptions = ParseArgs(strings)
      maybeOptions.getOrElse(ConfigOptions())
    }

    describe("when no debug flag") {
      val configOptions = invokeWithArgument("")
      it("debug should be false") {
        assertResult(false)(configOptions.contains(Debug()))
      }
    }
    describe("when long debug flag") {
      val configOptions = invokeWithArgument("--debug")
      it("debug should be true") {
        assert(configOptions.contains(Debug()))
      }
    }
    describe("when short debug flag") {
      val configOptions = invokeWithArgument("-d")
      it("debug should be true") {
        assert(configOptions.contains(Debug()))
      }
    }
  }

  private def pathTo(value: String): String =
    Try(Resource(this, value))
      .map(_.getCanonicalPath)
      .getOrElse("[not-found]")

}
