package net.kemitix.thorp.core

import java.nio.file.{Path, Paths}

import cats.data.NonEmptyChain
import cats.effect.IO
import net.kemitix.thorp.core.ConfigValidator.validateConfig
import net.kemitix.thorp.core.ParseConfigFile.parseFile
import net.kemitix.thorp.domain.Config

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val sourceConfigFilename = ".thorp.config"
  private val userConfigFilename   = ".config/thorp.conf"
  private val globalConfig         = Paths.get("/etc/thorp.conf")
  private val userHome             = Paths.get(System.getProperty("user.home"))
  private val pwd                  = Paths.get(System.getenv("PWD"))

  def buildConfig(priorityOpts: ConfigOptions)
      : IO[Either[NonEmptyChain[ConfigValidation], Config]] = {
    val sources = ConfigQuery.sources(priorityOpts)
    for {
      sourceOptions <- SourceConfigLoader.loadSourceConfigs(sources)
      userOptions   <- userOptions(priorityOpts ++ sourceOptions)
      globalOptions <- globalOptions(priorityOpts ++ sourceOptions ++ userOptions)
      collected = priorityOpts ++ sourceOptions ++ userOptions ++ globalOptions
      config    = collateOptions(collected)
    } yield validateConfig(config).toEither
  }

  private val emptyConfig = IO(ConfigOptions())

  private def userOptions(priorityOpts: ConfigOptions) =
    if (ConfigQuery.ignoreUserOptions(priorityOpts)) emptyConfig
    else readFile(userHome, userConfigFilename)

  private def globalOptions(priorityOpts: ConfigOptions) =
    if (ConfigQuery.ignoreGlobalOptions(priorityOpts)) emptyConfig
    else parseFile(globalConfig)

  private def readFile(
      source: Path,
      filename: String
  ) =
    parseFile(source.resolve(filename))

  private def collateOptions(configOptions: ConfigOptions): Config =
    configOptions.options.foldLeft(Config())((c, co) => co.update(c))

}

object ConfigurationBuilder extends ConfigurationBuilder
