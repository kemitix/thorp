package net.kemitix.thorp.config

import java.nio.file.Paths
import java.util

import scala.jdk.CollectionConverters._
import net.kemitix.thorp.domain.Sources
import org.scalatest.FreeSpec

class ConfigQueryTest extends FreeSpec {

  def list(): util.List[ConfigOption]                     = List.empty.asJava
  def list(option: ConfigOption): util.List[ConfigOption] = List(option).asJava
  def list(option1: ConfigOption,
           option2: ConfigOption): util.List[ConfigOption] =
    List(option1, option2).asJava

  "show version" - {
    "when is set" - {
      "should be true" in {
        val result =
          ConfigQuery.showVersion(
            ConfigOptions.create(list(ConfigOption.version())))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.showVersion(ConfigOptions.create(list()))
        assertResult(false)(result)
      }
    }
  }
  "batch mode" - {
    "when is set" - {
      "should be true" in {
        val result =
          ConfigQuery.batchMode(
            ConfigOptions.create(list(ConfigOption.batchMode())))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.batchMode(ConfigOptions.empty())
        assertResult(false)(result)
      }
    }
  }
  "ignore user options" - {
    "when is set" - {
      "should be true" in {
        val result = ConfigQuery.ignoreUserOptions(
          ConfigOptions.create(list(ConfigOption.ignoreUserOptions())))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.ignoreUserOptions(ConfigOptions.empty())
        assertResult(false)(result)
      }
    }
  }
  "ignore global options" - {
    "when is set" - {
      "should be true" in {
        val result = ConfigQuery.ignoreGlobalOptions(
          ConfigOptions.create(list(ConfigOption.ignoreGlobalOptions())))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.ignoreGlobalOptions(ConfigOptions.empty())
        assertResult(false)(result)
      }
    }
  }
  "sources" - {
    val pathA = Paths.get("a-path")
    val pathB = Paths.get("b-path")
    "when not set" - {
      "should have current dir" in {
        val pwd      = Paths.get(System.getenv("PWD"))
        val expected = Sources.create(List(pwd).asJava)
        val result   = ConfigQuery.sources(ConfigOptions.empty())
        assertResult(expected)(result)
      }
    }
    "when is set once" - {
      "should have one source" in {
        val expected = Sources.create(List(pathA).asJava)
        val result =
          ConfigQuery.sources(
            ConfigOptions.create(list(ConfigOption.source(pathA))))
        assertResult(expected)(result)
      }
    }
    "when is set twice" - {
      "should have two sources" in {
        val expected = Sources.create(List(pathA, pathB).asJava)
        val result = ConfigQuery.sources(
          ConfigOptions.create(
            list(ConfigOption.source(pathA), ConfigOption.source(pathB))))
        assertResult(expected)(result)
      }
    }
  }

}
