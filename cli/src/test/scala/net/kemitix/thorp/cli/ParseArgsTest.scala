package net.kemitix.thorp.cli

import net.kemitix.thorp.core.ConfigOption.Debug
import net.kemitix.thorp.core.{ConfigOptions, Resource}
import org.scalatest.FunSpec

import scala.util.Try

class ParseArgsTest extends FunSpec {

  val source = Resource(this, "")

  describe("parse - source") {
    def invokeWithSource(path: String) =
      ParseArgs(List("--source", path, "--bucket", "bucket"))

    describe("when source is a directory") {
      val result = invokeWithSource(pathTo("."))
      it("should succeed") {
        assert(result.isDefined)
      }
    }
    describe("when source is a relative path to a directory") {
      it("should succeed") {pending}
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
