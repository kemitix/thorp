package net.kemitix.s3thorp.cli

import net.kemitix.s3thorp.core.Resource
import net.kemitix.s3thorp.domain.{Bucket, Config}
import org.scalatest.FunSpec

import scala.util.Try

class ParseArgsTest extends FunSpec {

  val source = Resource(this, "")
  val defaultConfig: Config = Config(source = source)

  describe("parse - source") {
    def invokeWithSource(path: String) =
      ParseArgs(List("--source", path, "--bucket", "bucket"), defaultConfig)

    describe("when source is a directory") {
      val result = invokeWithSource(pathTo("."))
      it("should succeed") {
        assert(result.isDefined)
      }
    }
    describe("when source is a file") {
      val result = invokeWithSource(pathTo("ParseArgs.class"))
      it("should fail") {
        assert(result.isEmpty)
      }
    }
    describe("when source is not found") {
      val result = invokeWithSource(pathTo("not-found"))
      it("should fail") {
        assert(result.isEmpty)
      }
    }
    describe("when source is a relative path to a directory") {
      it("should succeed") {pending}
    }
    describe("when source is a relative path to a file") {
      it("should fail") {pending}
    }
    describe("when source is a relative path to a missing path") {
      it("should fail") {pending}
    }
  }

  describe("parse - debug") {
    def invokeWithDebug(debug: String) = {
      val strings = List("--source", pathTo("."), "--bucket", "bucket", debug)
          .filter(_ != "")
      ParseArgs(strings, defaultConfig).map(_.debug)
    }

    describe("when no debug flag") {
      val debugFlag = invokeWithDebug("")
      it("debug should be false") {
        assert(debugFlag.contains(false))
      }
    }
    describe("when long debug flag") {
      val debugFlag = invokeWithDebug("--debug")
      it("debug should be true") {
        assert(debugFlag.contains(true))
      }
    }
    describe("when short debug flag") {
      val debugFlag = invokeWithDebug("-d")
      it("debug should be true") {
        assert(debugFlag.contains(true))
      }
    }
  }

  private def pathTo(value: String): String =
    Try(Resource(this, value))
      .map(_.getCanonicalPath)
      .getOrElse("[not-found]")

}
