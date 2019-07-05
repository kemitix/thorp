package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.domain.Sources
import org.scalatest.FunSpec

class ConfigOptionTest extends FunSpec {

  private val source = Resource(this, "")
  private val sourcePath = source.toPath

  describe("when more than one source") {
    val path1 = sourcePath.resolve("path1")
    val path2 = sourcePath.resolve("path2")
    val configOptions = ConfigOptions(List(
      ConfigOption.Source(path1),
      ConfigOption.Source(path2),
      ConfigOption.Bucket("bucket"),
      ConfigOption.IgnoreGlobalOptions,
      ConfigOption.IgnoreUserOptions
    ))
    val args = ConfigurationBuilder.buildConfig(configOptions).unsafeRunSync
    it("should successfully parse") {
      assert(args.isRight, args)
    }
    it("should preserve their order") {
      val expected = Sources(List(path1, path2))
      assertResult(expected)(ConfigQuery.sources(configOptions))
    }
  }
}
