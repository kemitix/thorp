package net.kemitix.thorp.core

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}

import net.kemitix.thorp.domain.{Bucket, Config, Sources}
import org.scalatest.FunSpec

class ConfigurationBuilderTest extends FunSpec {

  val validBase =
    ConfigOptions(List(
      ConfigOption.Bucket("aBucket"),
      ConfigOption.IgnoreUserOptions,
      ConfigOption.IgnoreGlobalOptions
    ))

  val aBucket = Bucket("aBucket")

  val aSource: Path = Resource(this, "upload").toPath
  val path1: Path = Resource(this, "path1").toPath
  val path2: Path = Resource(this, "path2").toPath
  val pwd: Path = Paths.get(System.getenv("PWD"))

  describe("when no source") {
    val configOptions = validBase
    it("should use the current (PWD) directory") {
      val expected = Right(Config(aBucket, sources = Sources(List(pwd))))
      val result = invoke(configOptions)
      assertResult(expected)(result)
    }
  }
  describe("when has a single source") {
    val source = ConfigOption.Source(aSource)
    val configOptions = validBase ++ ConfigOptions(List(source))
    it("should only include the source once") {
      val expected = List(aSource)
      val result = invoke(configOptions).right.get.sources.paths
      assertResult(expected)(result)
    }
    it("should use the single source") {
      val expected = Right(Config(aBucket, sources = Sources(List(aSource))))
      val result = invoke(configOptions)
      assertResult(expected)(result)
    }
  }
  describe("when has two sources") {
    val source1 = ConfigOption.Source(aSource)
    val source2 = ConfigOption.Source(path1)
    val configOptions = validBase ++ ConfigOptions(List(
      source1, source2
    ))
    it("should include both sources in correct order") {
      val expected = List(aSource, path1)
      val result = invoke(configOptions).right.get.sources.paths
      assertResult(expected)(result)
    }
  }

  def writeFile(directory: File, name: String, contents: List[String]): Unit = {
    directory.mkdirs
    val pw = new PrintWriter(filename(directory, name), "UTF-8")
    contents.foreach(pw.println)
    pw.close()
  }

  private def filename(directory: File, name: String) =
    directory.toPath.resolve(name).toFile

  def deleteFile(directory: File, name: String): Unit = {
    Files.delete(filename(directory, name).toPath)
    directory.delete
  }

  val thorpConfigFileName = ".thorp.config"

  describe("when source has thorp.config source to another source") {
    val currentSourceFile = resourcePath.resolve("parent1").toFile
    val currentSource = currentSourceFile.toPath
    val previousSource = path2
    describe("when settings are only in current") {
      it("should have bucket from current") {pending}
      it("should have prefix from current") {pending}
      it("should have filters from current for current source") {pending}
    }
    describe("when settings are in current and previous") {
      writeFile(currentSourceFile, thorpConfigFileName, List(s"source = $previousSource"))
      val source = ConfigOption.Source(currentSource)
      val configOptions = validBase ++ ConfigOptions(List(source))
      it("should include both sources") {
        val expected = List(currentSource, previousSource)
        val result = invoke(configOptions).right.get.sources.paths
        deleteFile(currentSourceFile, thorpConfigFileName)
        assertResult(expected)(result)
      }
      it("should have bucket from current only") {pending}
      it("should have prefix from current only") {pending}
      it("should have filters from both current and previous") {pending}
    }
    describe("when settings are only in previous") {
      it("should have bucket from previous") {pending}
      it("should have prefix from previous") {pending}
      it("should have filters from previous for previous source") {pending}
    }
  }

  describe("when source has thorp.config source to another source that does the same") {
    val currentSource = resourcePath.resolve("current")
    val currentSourceFile = currentSource.toFile
    val parentSource = resourcePath.resolve("parent")
    val parentSourceFile = parentSource.toFile
    val grandParentSource = path2
    writeFile(currentSourceFile, thorpConfigFileName, List(s"source = $parentSource"))
    writeFile(parentSourceFile, thorpConfigFileName, List(s"source = $grandParentSource"))
    val source = ConfigOption.Source(currentSource)
    val configOptions = validBase ++ ConfigOptions(List(source))
    it("should include all three sources") {
      val expected = List(currentSource, parentSource, grandParentSource)
      val result = invoke(configOptions).right.get.sources.paths
      deleteFile(currentSourceFile, thorpConfigFileName)
      deleteFile(parentSourceFile, thorpConfigFileName)
      assertResult(expected)(result)
    }
  }

  private def resourcePath = {
    Resource(this, ".").toPath
  }

  private def invoke(configOptions: ConfigOptions) =
    ConfigurationBuilder.buildConfig(configOptions).unsafeRunSync
}
