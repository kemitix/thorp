package net.kemitix.thorp.config

import net.kemitix.thorp.domain.Sources
import net.kemitix.thorp.filesystem.TemporaryFolder
import org.scalatest.FunSpec
import zio.DefaultRuntime

import scala.jdk.CollectionConverters._

class ConfigOptionTest extends FunSpec with TemporaryFolder {

  describe("when more than one source") {
    it("should preserve their order") {
      withDirectory(path1 => {
        withDirectory(path2 => {
          val configOptions = ConfigOptions.create(
            List[ConfigOption](
              ConfigOption.source(path1),
              ConfigOption.source(path2),
              ConfigOption.bucket("bucket"),
              ConfigOption.ignoreGlobalOptions(),
              ConfigOption.ignoreUserOptions()
            ).asJava)
          val expected = Sources.create(List(path1, path2).asJava)
          val result   = invoke(configOptions)
          assert(result.isRight, result)
          assertResult(expected)(ConfigQuery.sources(configOptions))
        })
      })
    }
  }

  private def invoke(configOptions: ConfigOptions) = {
    new DefaultRuntime {}.unsafeRunSync {
      ConfigurationBuilder.buildConfig(configOptions)
    }.toEither
  }
}
