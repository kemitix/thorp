package net.kemitix.thorp.core

import java.io.PrintWriter
import java.nio.file.{Path, Paths}

import net.kemitix.thorp.domain._
import org.scalatest.FunSpec

class ConfigurationBuilderTest extends FunSpec with TemporaryFolder {

  private val pwd: Path = Paths.get(System.getenv("PWD"))
  private val aBucket = Bucket("aBucket")
  private val coBucket: ConfigOption.Bucket = ConfigOption.Bucket(aBucket.name)
  private val thorpConfigFileName = ".thorp.config"

  @deprecated
  private val validBase =
    ConfigOptions(List(
      coBucket,
      ConfigOption.IgnoreUserOptions,
      ConfigOption.IgnoreGlobalOptions
    ))

  private def configOptions(options: ConfigOption*): ConfigOptions =
    ConfigOptions(List(
        ConfigOption.IgnoreUserOptions,
        ConfigOption.IgnoreGlobalOptions
      ) ++ options)

  private def writeFile(directory: Path, name: String, contents: String*): Unit = {
    directory.toFile.mkdirs
    val pw = new PrintWriter(directory.resolve(name).toFile, "UTF-8")
    contents.foreach(pw.println)
    pw.close()
  }


  describe("when no source") {
    it("should use the current (PWD) directory") {
      val expected = Right(Config(aBucket, sources = Sources(List(pwd))))
      val options = configOptions(coBucket)
      val result = invoke(options)
      assert(result.isRight, result)
      assertResult(expected)(result)
    }
  }
  describe("when has a single source with no .thorp.config") {
    it("should only include the source once") {
      withDirectory(aSource => {
        val expected = Sources(List(aSource))
        val options = configOptions(ConfigOption.Source(aSource), coBucket)
        val result = invoke(options)
        assert(result.isRight, result)
        assertResult(expected)(result.right.get.sources)
      })
    }
  }
  describe("when has two sources") {
    it("should include both sources in order") {
      withDirectory(currentSource => {
        withDirectory(previousSource => {
          val expected = List(currentSource, previousSource)
          val options = configOptions(
            ConfigOption.Source(currentSource),
            ConfigOption.Source(previousSource),
            coBucket)
          val result = invoke(options)
          assert(result.isRight, result)
          assertResult(expected)(result.right.get.sources.paths)
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
          val expected = List(currentSource, previousSource)
          val options = configOptions(
            ConfigOption.Source(currentSource),
            coBucket)
          val result = invoke(options)
          assert(result.isRight, result)
          assertResult(expected)(result.right.get.sources.paths)
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
            val expectedSources = Sources(List(currentSource, previousSource))
            // should have bucket from current only
            val expectedBuckets = Bucket("current-bucket")
            // should have prefix from current only
            val expectedPrefixes = RemoteKey("current-prefix")
            // should have filters from both sources
            val expectedFilters = List(
              Filter.Exclude("previous-exclude"),
              Filter.Include("previous-include"),
              Filter.Exclude("current-exclude"),
              Filter.Include("current-include"))
            val options = configOptions(ConfigOption.Source(currentSource))
            val result = invoke(options)
            assert(result.isRight, result)
            val config = result.right.get
            assertResult(expectedSources)(config.sources)
            assertResult(expectedBuckets)(config.bucket)
            assertResult(expectedPrefixes)(config.prefix)
            assertResult(expectedFilters)(config.filters)
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
            val expected = List(currentSource, parentSource, grandParentSource)
            val options = configOptions(
              ConfigOption.Source(currentSource),
              coBucket)
            val result = invoke(options)
            assert(result.isRight, result)
            assertResult(expected)(result.right.get.sources.paths)
          })
        })
      })
    }
  }

  private def invoke(configOptions: ConfigOptions) =
    ConfigurationBuilder.buildConfig(configOptions).unsafeRunSync
}
