package net.kemitix.thorp.core

import java.nio.file.{Path, Paths}

import net.kemitix.thorp.domain._
import org.scalatest.FunSpec

class ConfigurationBuilderTest extends FunSpec with TemporaryFolder {

  private val pwd: Path = Paths.get(System.getenv("PWD"))
  private val aBucket = Bucket("aBucket")
  private val coBucket: ConfigOption.Bucket = ConfigOption.Bucket(aBucket.name)
  private val thorpConfigFileName = ".thorp.config"

  private def configOptions(options: ConfigOption*): ConfigOptions =
    ConfigOptions(List(
        ConfigOption.IgnoreUserOptions,
        ConfigOption.IgnoreGlobalOptions
      ) ++ options)

  describe("when no source") {
    it("should use the current (PWD) directory") {
      val expected = Right(Config(aBucket, sources = Sources(List(pwd))))
      val options = configOptions(coBucket)
      val result = invoke(options)
      assertResult(expected)(result)
    }
  }
  describe("when has a single source with no .thorp.config") {
    it("should only include the source once") {
      withDirectory(aSource => {
        val expected = Right(Sources(List(aSource)))
        val options = configOptions(ConfigOption.Source(aSource), coBucket)
        val result = invoke(options).map(_.sources)
        assertResult(expected)(result)
      })
    }
  }
  describe("when has two sources") {
    it("should include both sources in order") {
      withDirectory(currentSource => {
        withDirectory(previousSource => {
          val expected = Right(List(currentSource, previousSource))
          val options = configOptions(
            ConfigOption.Source(currentSource),
            ConfigOption.Source(previousSource),
            coBucket)
          val result = invoke(options).map(_.sources.paths)
          assertResult(expected)(result)
        })
      })
    }
  }
  describe("when current source has .thorp.config with source to another") {
    it("should include both sources in order") {
      withDirectory(currentSource => {
        withDirectory(previousSource => {
          writeFile(currentSource, thorpConfigFileName,
            s"source = $previousSource")
          val expected = Right(List(currentSource, previousSource))
          val options = configOptions(
            ConfigOption.Source(currentSource),
            coBucket)
          val result = invoke(options).map(_.sources.paths)
          assertResult(expected)(result)
        })
      })
    }
    describe("when settings are in current and previous") {
      it("should include some settings from both sources and some from only current") {
        withDirectory(previousSource => {
          withDirectory(currentSource => {
            writeFile(currentSource, thorpConfigFileName,
              s"source = $previousSource",
              "bucket = current-bucket",
              "prefix = current-prefix",
              "include = current-include",
              "exclude = current-exclude")
            writeFile(previousSource, thorpConfigFileName,
              "bucket = previous-bucket",
              "prefix = previous-prefix",
              "include = previous-include",
              "exclude = previous-exclude")
            // should have both sources in order
            val expectedSources = Right(Sources(List(currentSource, previousSource)))
            // should have bucket from current only
            val expectedBuckets = Right(Bucket("current-bucket"))
            // should have prefix from current only
            val expectedPrefixes = Right(RemoteKey("current-prefix"))
            // should have filters from both sources
            val expectedFilters = Right(List(
              Filter.Exclude("previous-exclude"),
              Filter.Include("previous-include"),
              Filter.Exclude("current-exclude"),
              Filter.Include("current-include")))
            val options = configOptions(ConfigOption.Source(currentSource))
            val result = invoke(options)
            assertResult(expectedSources)(result.map(_.sources))
            assertResult(expectedBuckets)(result.map(_.bucket))
            assertResult(expectedPrefixes)(result.map(_.prefix))
            assertResult(expectedFilters)(result.map(_.filters))
          })
        })
      }
    }
  }

  describe("when source has thorp.config source to another source that does the same") {
    it("should include all three sources") {
      withDirectory(currentSource => {
        withDirectory(parentSource => {
          writeFile(currentSource, thorpConfigFileName, s"source = $parentSource")
          withDirectory(grandParentSource => {
            writeFile(parentSource, thorpConfigFileName, s"source = $grandParentSource")
            val expected = Right(List(currentSource, parentSource, grandParentSource))
            val options = configOptions(
              ConfigOption.Source(currentSource),
              coBucket)
            val result = invoke(options).map(_.sources.paths)
            assertResult(expected)(result)
          })
        })
      })
    }
  }

  private def invoke(configOptions: ConfigOptions) =
    ConfigurationBuilder.buildConfig(configOptions).unsafeRunSync
}
