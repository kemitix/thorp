package net.kemitix.thorp.core

import java.nio.file.Paths

import cats.data.NonEmptyChain
import cats.effect.IO
import net.kemitix.thorp.core.ConfigValidator.validateConfig
import net.kemitix.thorp.core.ParseConfigFile.parseFile
import net.kemitix.thorp.core.ConfigOptions.options
import net.kemitix.thorp.domain.Config

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val userConfigFilename = ".config/thorp.conf"
  private val globalConfig       = Paths.get("/etc/thorp.conf")
  private val userHome           = Paths.get(System.getProperty("user.home"))

  def buildConfig(priorityOpts: ConfigOptions)
    : IO[Either[NonEmptyChain[ConfigValidation], Config]] = {
    val sources = ConfigQuery.sources(priorityOpts)
    for {
      sourceOpts <- SourceConfigLoader.loadSourceConfigs(sources)
      userOpts   <- userOptions(priorityOpts ++ sourceOpts)
      globalOpts <- globalOptions(priorityOpts ++ sourceOpts ++ userOpts)
      collected = priorityOpts ++ sourceOpts ++ userOpts ++ globalOpts
      config    = collateOptions(collected)
    } yield validateConfig(config).toEither
  }

  private val emptyConfig = IO(ConfigOptions())

  private def userOptions(priorityOpts: ConfigOptions) =
    if (ConfigQuery.ignoreUserOptions(priorityOpts)) emptyConfig
    else parseFile(userHome.resolve(userConfigFilename))

  private def globalOptions(priorityOpts: ConfigOptions) =
    if (ConfigQuery.ignoreGlobalOptions(priorityOpts)) emptyConfig
    else parseFile(globalConfig)

  private def collateOptions(configOptions: ConfigOptions): Config =
    options
      .get(configOptions)
      .foldLeft(Config()) { (config, configOption) =>
        configOption.update(config)
      }

}

object ConfigurationBuilder extends ConfigurationBuilder
