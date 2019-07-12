package net.kemitix.thorp.core

import java.nio.file.{Files, Path, Paths}

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

  def buildConfig(priorityOptions: ConfigOptions): IO[Either[NonEmptyChain[ConfigValidation], Config]] = {
    val sources = ConfigQuery.sources(priorityOptions)
    for {
      sourceOptions <- SourceConfigLoader.loadSourceConfigs(sources)
      userOptions   <- userOptions(priorityOptions ++ sourceOptions)
      globalOptions <- globalOptions(priorityOptions ++ sourceOptions ++ userOptions)
      collected = priorityOptions ++ sourceOptions ++ userOptions ++ globalOptions
      config    = collateOptions(collected)
    } yield validateConfig(config).toEither
  }

  private def userOptions(higherPriorityOptions: ConfigOptions): IO[ConfigOptions] =
    if (ConfigQuery.ignoreUserOptions(higherPriorityOptions)) IO(ConfigOptions())
    else readFile(userHome, ".config/thorp.conf")

  private def globalOptions(higherPriorityOptions: ConfigOptions): IO[ConfigOptions] =
    if (ConfigQuery.ignoreGlobalOptions(higherPriorityOptions)) IO(ConfigOptions())
    else parseFile(Paths.get("/etc/thorp.conf"))

  private def userHome = Paths.get(System.getProperty("user.home"))

  private def readFile(source: Path, filename: String): IO[ConfigOptions] =
    parseFile(source.resolve(filename))

  private def collateOptions(configOptions: ConfigOptions): Config =
    configOptions.options.foldLeft(Config())((c, co) => co.update(c))

}

object ConfigurationBuilder extends ConfigurationBuilder
