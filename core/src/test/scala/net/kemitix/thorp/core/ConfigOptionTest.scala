package net.kemitix.thorp.core

import net.kemitix.thorp.domain.Sources
import org.scalatest.FunSpec
import zio.DefaultRuntime

class ConfigOptionTest extends FunSpec with TemporaryFolder {

  describe("when more than one source") {
    it("should preserve their order") {
      withDirectory(path1 => {
        withDirectory(path2 => {
          val configOptions = ConfigOptions(
            List(
              ConfigOption.Source(path1),
              ConfigOption.Source(path2),
              ConfigOption.Bucket("bucket"),
              ConfigOption.IgnoreGlobalOptions,
              ConfigOption.IgnoreUserOptions
            ))
          val expected = Sources(List(path1, path2))
          val result   = invoke(configOptions)
          assert(result.isRight, result)
          assertResult(expected)(ConfigQuery.sources(configOptions))
        })
      })
    }
  }

  private def invoke(configOptions: ConfigOptions) = {
    val runtime = new DefaultRuntime {}
    runtime.unsafeRunSync {
      ConfigurationBuilder.buildConfig(configOptions)
    }.toEither
  }
}
