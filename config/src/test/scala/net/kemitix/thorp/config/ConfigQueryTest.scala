package net.kemitix.thorp.config

import java.nio.file.Paths

import net.kemitix.thorp.domain.Sources
import org.scalatest.FreeSpec

class ConfigQueryTest extends FreeSpec {

  "show version" - {
    "when is set" - {
      "should be true" in {
        val result =
          ConfigQuery.showVersion(ConfigOptions(List(ConfigOption.Version)))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.showVersion(ConfigOptions(List()))
        assertResult(false)(result)
      }
    }
  }
  "batch mode" - {
    "when is set" - {
      "should be true" in {
        val result =
          ConfigQuery.batchMode(ConfigOptions(List(ConfigOption.BatchMode)))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.batchMode(ConfigOptions(List()))
        assertResult(false)(result)
      }
    }
  }
  "ignore user options" - {
    "when is set" - {
      "should be true" in {
        val result = ConfigQuery.ignoreUserOptions(
          ConfigOptions(List(ConfigOption.IgnoreUserOptions)))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.ignoreUserOptions(ConfigOptions(List()))
        assertResult(false)(result)
      }
    }
  }
  "ignore global options" - {
    "when is set" - {
      "should be true" in {
        val result = ConfigQuery.ignoreGlobalOptions(
          ConfigOptions(List(ConfigOption.IgnoreGlobalOptions)))
        assertResult(true)(result)
      }
    }
    "when not set" - {
      "should be false" in {
        val result = ConfigQuery.ignoreGlobalOptions(ConfigOptions(List()))
        assertResult(false)(result)
      }
    }
  }
  "sources" - {
    val pathA = Paths.get("a-path")
    val pathB = Paths.get("b-path")
    "when not set" - {
      "should have current dir" - {
        val pwd      = Paths.get(System.getenv("PWD"))
        val expected = Sources(List(pwd))
        val result   = ConfigQuery.sources(ConfigOptions(List()))
        assertResult(expected)(result)
      }
    }
    "when is set once" - {
      "should have one source" in {
        val expected = Sources(List(pathA))
        val result =
          ConfigQuery.sources(ConfigOptions(List(ConfigOption.Source(pathA))))
        assertResult(expected)(result)
      }
    }
    "when is set twice" - {
      "should have two sources" in {
        val expected = Sources(List(pathA, pathB))
        val result = ConfigQuery.sources(
          ConfigOptions(
            List(ConfigOption.Source(pathA), ConfigOption.Source(pathB))))
        assertResult(expected)(result)
      }
    }
  }

}
