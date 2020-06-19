package net.kemitix.thorp.config

import java.io.File

import zio.Task

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val userConfigFile = ".config/thorp.conf"
  private val globalConfig   = new File("/etc/thorp.conf")
  private val userHome       = new File(System.getProperty("user.home"))

  def buildConfig(priorityOpts: ConfigOptions): Task[Configuration] =
    (getConfigOptions(priorityOpts).map(collateOptions) >>=
      ConfigValidator.validateConfig)

  private def getConfigOptions(
      priorityOpts: ConfigOptions): Task[ConfigOptions] = {
    val sourceOpts =
      SourceConfigLoader.loadSourceConfigs(ConfigQuery.sources(priorityOpts))
    val userOpts   = userOptions(priorityOpts ++ sourceOpts)
    val globalOpts = globalOptions(priorityOpts ++ sourceOpts ++ userOpts)
    Task(priorityOpts ++ sourceOpts ++ userOpts ++ globalOpts)
  }

  private def userOptions(priorityOpts: ConfigOptions): ConfigOptions =
    if (ConfigQuery.ignoreUserOptions(priorityOpts))
      ConfigOptions.empty
    else ParseConfigFile.parseFile(new File(userHome, userConfigFile))

  private def globalOptions(priorityOpts: ConfigOptions): ConfigOptions =
    if (ConfigQuery.ignoreGlobalOptions(priorityOpts))
      ConfigOptions.empty
    else ParseConfigFile.parseFile(globalConfig)

  private def collateOptions(configOptions: ConfigOptions): Configuration =
    ConfigOptions.options
      .get(configOptions)
      .foldLeft(Configuration.empty) { (config, configOption) =>
        configOption.update(config)
      }

}

object ConfigurationBuilder extends ConfigurationBuilder
