package net.kemitix.thorp.core

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
    val paths = configOptions.options.flatMap( {
      case ConfigOption.Source(sourcePath) => Some(sourcePath)
      case _ => None
    })
    Sources(paths)
  }
}

object ConfigQuery extends ConfigQuery
