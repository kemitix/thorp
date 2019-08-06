package net.kemitix.thorp.config

import java.nio.file.Paths

import net.kemitix.thorp.domain.Sources

trait ConfigQuery {

  def showVersion(configOptions: ConfigOptions): Boolean =
    ConfigOptions.contains(ConfigOption.Version)(configOptions)

  def batchMode(configOptions: ConfigOptions): Boolean =
    ConfigOptions.contains(ConfigOption.BatchMode)(configOptions)

  def ignoreUserOptions(configOptions: ConfigOptions): Boolean =
    ConfigOptions.contains(ConfigOption.IgnoreUserOptions)(configOptions)

  def ignoreGlobalOptions(configOptions: ConfigOptions): Boolean =
    ConfigOptions.contains(ConfigOption.IgnoreGlobalOptions)(configOptions)

  def sources(configOptions: ConfigOptions): Sources = {
    val explicitPaths = configOptions.options.flatMap {
      case ConfigOption.Source(sourcePath) => List(sourcePath)
      case _                               => List.empty
    }
    val paths = explicitPaths match {
      case List() => List(Paths.get(System.getenv("PWD")))
      case _      => explicitPaths
    }
    Sources(paths)
  }

}

object ConfigQuery extends ConfigQuery
