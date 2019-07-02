package net.kemitix.thorp.core

trait ConfigQuery {

  def showVersion(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.Version

  def batchMode(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.BatchMode

  def ignoreUserOptions(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.IgnoreUserOptions

  def ignoreGlobalOptions(configOptions: ConfigOptions): Boolean =
    configOptions contains ConfigOption.IgnoreGlobalOptions

}

object ConfigQuery extends ConfigQuery
