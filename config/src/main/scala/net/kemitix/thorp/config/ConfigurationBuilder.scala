package net.kemitix.thorp.config

import java.io.File

import zio.ZIO

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val userConfigFile = ".config/thorp.conf"
  private val globalConfig   = new File("/etc/thorp.conf")
  private val userHome       = new File(System.getProperty("user.home"))

  def buildConfig(priorityOpts: ConfigOptions)
    : ZIO[Any, ConfigValidationException, Configuration] =
    (getConfigOptions(priorityOpts).map(collateOptions) >>=
      ConfigValidator.validateConfig)
      .catchAll(errors => ZIO.fail(ConfigValidationException(errors)))

  private def getConfigOptions(priorityOpts: ConfigOptions)
    : ZIO[Any, Seq[ConfigValidation], ConfigOptions] =
    for {
      sourceOpts <- SourceConfigLoader.loadSourceConfigs(
        ConfigQuery.sources(priorityOpts))
      userOpts   <- userOptions(priorityOpts ++ sourceOpts)
      globalOpts <- globalOptions(priorityOpts ++ sourceOpts ++ userOpts)
    } yield priorityOpts ++ sourceOpts ++ userOpts ++ globalOpts

  private val emptyConfig = ZIO.succeed(ConfigOptions.empty)

  private def userOptions(priorityOpts: ConfigOptions)
    : ZIO[Any, Seq[ConfigValidation], ConfigOptions] =
    if (ConfigQuery.ignoreUserOptions(priorityOpts)) emptyConfig
    else ParseConfigFile.parseFile(new File(userHome, userConfigFile))

  private def globalOptions(priorityOpts: ConfigOptions)
    : ZIO[Any, Seq[ConfigValidation], ConfigOptions] =
    if (ConfigQuery.ignoreGlobalOptions(priorityOpts)) emptyConfig
    else ParseConfigFile.parseFile(globalConfig)

  private def collateOptions(configOptions: ConfigOptions): Configuration =
    ConfigOptions.options
      .get(configOptions)
      .foldLeft(Configuration.empty) { (config, configOption) =>
        configOption.update(config)
      }

}

object ConfigurationBuilder extends ConfigurationBuilder
