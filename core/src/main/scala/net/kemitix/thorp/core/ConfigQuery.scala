package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.domain.Sources

trait ConfigQuery {

  def showVersion(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.Version

  def batchMode(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.BatchMode

  def ignoreUserOptions(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.IgnoreUserOptions

  def ignoreGlobalOptions(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.IgnoreGlobalOptions

  def sources(configOptions: ConfigOptions): Sources = {
    val paths = configOptions.options.flatMap {
      case ConfigOption.Source(sourcePath) => Some(sourcePath)
      case _ => None
    }
    Sources(paths match {
      case List() => List(Paths.get(System.getenv("PWD")))
      case _ => paths
    })
  }

}

object ConfigQuery extends ConfigQuery
