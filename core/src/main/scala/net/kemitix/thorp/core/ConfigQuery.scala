package net.kemitix.thorp.core

trait ConfigQuery {

  def showVersion(configOptions: Seq[ConfigOption]): Boolean =
    configOptions.exists {
      case ConfigOption.Version => true
      case _ => false
    }

  def ignoreUserOptions(configOptions: Seq[ConfigOption]): Boolean =
    configOptions.exists {
      case ConfigOption.IgnoreUserOptions => true
      case _ => false
    }

  def ignoreGlobalOptions(configOptions: Seq[ConfigOption]): Boolean =
    configOptions.exists {
      case ConfigOption.IgnoreGlobalOptions => true
      case _ => false
    }

}

object ConfigQuery extends ConfigQuery
