package net.kemitix.thorp.core

trait ConfigQuery {

  def showVersion(configOptions: Seq[ConfigOption]): Boolean =
    configOptions contains ConfigOption.Version

  def batchMode(configOptions: Seq[ConfigOption]): Boolean =
    configOptions contains ConfigOption.BatchMode

  def ignoreUserOptions(configOptions: Seq[ConfigOption]): Boolean =
    configOptions contains ConfigOption.IgnoreUserOptions

  def ignoreGlobalOptions(configOptions: Seq[ConfigOption]): Boolean =
    configOptions contains ConfigOption.IgnoreGlobalOptions

}

object ConfigQuery extends ConfigQuery
