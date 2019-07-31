package net.kemitix.thorp.config

import java.nio.file.Paths

import net.kemitix.thorp.filesystem.FileSystem
import zio.ZIO

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val userConfigFilename = ".config/thorp.conf"
  private val globalConfig       = Paths.get("/etc/thorp.conf")
  private val userHome           = Paths.get(System.getProperty("user.home"))

  def buildConfig(priorityOpts: ConfigOptions)
    : ZIO[FileSystem, ConfigValidationException, Configuration] =
    (getConfigOptions(priorityOpts).map(collateOptions) >>=
      ConfigValidator.validateConfig)
      .catchAll(errors => ZIO.fail(ConfigValidationException(errors)))

  private def getConfigOptions(priorityOpts: ConfigOptions) =
    for {
      sourceOpts <- SourceConfigLoader.loadSourceConfigs(
        ConfigQuery.sources(priorityOpts))
      userOpts   <- userOptions(priorityOpts ++ sourceOpts)
      globalOpts <- globalOptions(priorityOpts ++ sourceOpts ++ userOpts)
    } yield priorityOpts ++ sourceOpts ++ userOpts ++ globalOpts

  private val emptyConfig = ZIO.succeed(ConfigOptions())

  private def userOptions(priorityOpts: ConfigOptions) =
    if (ConfigQuery.ignoreUserOptions(priorityOpts)) emptyConfig
    else ParseConfigFile.parseFile(userHome.resolve(userConfigFilename))

  private def globalOptions(priorityOpts: ConfigOptions) =
    if (ConfigQuery.ignoreGlobalOptions(priorityOpts)) emptyConfig
    else ParseConfigFile.parseFile(globalConfig)

  private def collateOptions(configOptions: ConfigOptions): Configuration =
    ConfigOptions.options
      .get(configOptions)
      .foldLeft(Configuration()) { (config, configOption) =>
        configOption.update(config)
      }

}

object ConfigurationBuilder extends ConfigurationBuilder
