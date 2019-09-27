package net.kemitix.thorp.cli

import java.nio.file.Paths

import net.kemitix.thorp.config.ConfigOption.Debug
import net.kemitix.thorp.config.{ConfigOptions, ConfigQuery}
import net.kemitix.thorp.filesystem.Resource
import org.scalatest.FunSpec
import zio.DefaultRuntime

import scala.util.Try

class CliArgsTest extends FunSpec {

  private val runtime = new DefaultRuntime {}

  val source = Resource(this, "")

  describe("parse - source") {
    def invokeWithSource(path: String) =
      invoke(List("--source", path, "--bucket", "bucket"))

    describe("when source is a directory") {
      it("should succeed") {
        val result = invokeWithSource(pathTo("."))
        assert(result.isDefined)
      }
    }
    describe("when source is a relative path to a directory") {
      val result = invokeWithSource(pathTo("."))
      it("should succeed") { pending }
    }
    describe("when there are multiple sources") {
      val args =
        List("--source", "path1", "--source", "path2", "--bucket", "bucket")
      it("should get multiple sources") {
        val expected      = Some(Set("path1", "path2").map(Paths.get(_)))
        val configOptions = invoke(args)
        val result        = configOptions.map(ConfigQuery.sources(_).paths.toSet)
        assertResult(expected)(result)
      }
    }
  }

  describe("parse - debug") {
    def invokeWithArgument(arg: String): ConfigOptions = {
      val strings = List("--source", pathTo("."), "--bucket", "bucket", arg)
        .filter(_ != "")
      val maybeOptions = invoke(strings)
      maybeOptions.getOrElse(ConfigOptions.empty)
    }

    val containsDebug = ConfigOptions.contains(Debug())(_)

    describe("when no debug flag") {
      val configOptions = invokeWithArgument("")
      it("debug should be false") {
        assertResult(false)(containsDebug(configOptions))
      }
    }
    describe("when long debug flag") {
      val configOptions = invokeWithArgument("--debug")
      it("debug should be true") {
        assert(containsDebug(configOptions))
      }
    }
    describe("when short debug flag") {
      val configOptions = invokeWithArgument("-d")
      it("debug should be true") {
        assert(containsDebug(configOptions))
      }
    }
  }

  describe("parse - parallel") {
    def invokeWithArguments(args: List[String]): ConfigOptions = {
      val strings = List("--source", pathTo("."), "--bucket", "bucket")
        .concat(args)
        .filter(_ != "")
      val maybeOptions = invoke(strings)
      maybeOptions.getOrElse(ConfigOptions.empty)
    }

    describe("when no parallel parameter") {
      val configOptions = invokeWithArguments(List.empty[String])
      it("should have parallel of 1") {
        assertResult(1)(ConfigOptions.parallel(configOptions))
      }
    }
    describe("when parallel parameter given") {
      val configOptions = invokeWithArguments(List("--parallel", "5"))
      it("should have parallel of 5") {
        assertResult(5)(ConfigOptions.parallel(configOptions))
      }
    }
  }

  private def pathTo(value: String): String =
    Try(Resource(this, value))
      .map(_.getCanonicalPath)
      .getOrElse("[not-found]")

  private def invoke(args: List[String]) =
    runtime
      .unsafeRunSync {
        CliArgs.parse(args)
      }
      .toEither
      .toOption

}
