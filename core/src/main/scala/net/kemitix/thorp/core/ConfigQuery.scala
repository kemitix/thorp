package net.kemitix.thorp.core

import java.nio.file.Path

trait ConfigQuery {

  def showVersion(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.Version

  def batchMode(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.BatchMode

  def ignoreUserOptions(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.IgnoreUserOptions

  def ignoreGlobalOptions(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.IgnoreGlobalOptions

  def sources(configOptions: ConfigOptions): List[Path] =
    configOptions.options.flatMap {
      case s: ConfigOption.Source => Some(s)
      case _ => None
    }.map { s => s.path }
}

object ConfigQuery extends ConfigQuery
