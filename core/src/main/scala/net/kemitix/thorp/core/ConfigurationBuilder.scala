package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.core.ConfigOptions.options
import net.kemitix.thorp.core.ConfigValidator.validateConfig
import net.kemitix.thorp.core.ParseConfigFile.parseFile
import net.kemitix.thorp.domain.LegacyConfig
import zio.IO

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val userConfigFilename = ".config/thorp.conf"
  private val globalConfig       = Paths.get("/etc/thorp.conf")
  private val userHome           = Paths.get(System.getProperty("user.home"))

  def buildConfig(
      priorityOpts: ConfigOptions): IO[List[ConfigValidation], LegacyConfig] =
    for {
      config <- getConfigOptions(priorityOpts).map(collateOptions)
      valid  <- validateConfig(config)
    } yield valid

  private def getConfigOptions(
      priorityOpts: ConfigOptions): IO[List[ConfigValidation], ConfigOptions] =
    for {
      sourceOpts <- SourceConfigLoader.loadSourceConfigs(
        ConfigQuery.sources(priorityOpts))
      userOpts   <- userOptions(priorityOpts ++ sourceOpts)
      globalOpts <- globalOptions(priorityOpts ++ sourceOpts ++ userOpts)
    } yield priorityOpts ++ sourceOpts ++ userOpts ++ globalOpts

  private val emptyConfig = IO.succeed(ConfigOptions())

  private def userOptions(
      priorityOpts: ConfigOptions): IO[List[ConfigValidation], ConfigOptions] =
    if (ConfigQuery.ignoreUserOptions(priorityOpts)) emptyConfig
    else parseFile(userHome.resolve(userConfigFilename))

  private def globalOptions(
      priorityOpts: ConfigOptions): IO[List[ConfigValidation], ConfigOptions] =
    if (ConfigQuery.ignoreGlobalOptions(priorityOpts)) emptyConfig
    else parseFile(globalConfig)

  private def collateOptions(configOptions: ConfigOptions): LegacyConfig =
    options
      .get(configOptions)
      .foldLeft(LegacyConfig()) { (config, configOption) =>
        configOption.update(config)
      }

}

object ConfigurationBuilder extends ConfigurationBuilder
