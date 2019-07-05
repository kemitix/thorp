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

  private val pwd: Path = Paths.get(System.getenv("PWD"))

  private val defaultConfig: Config = Config(source = pwd)

  def buildConfig(priorityOptions: ConfigOptions): IO[Either[NonEmptyChain[ConfigValidation], Config]] = {
    val source = findSource(priorityOptions)
    for {
      sourceOptions <- sourceOptions(source)
      userOptions <- userOptions(priorityOptions ++ sourceOptions)
      globalOptions <- globalOptions(priorityOptions ++ sourceOptions ++ userOptions)
      collected = priorityOptions ++ sourceOptions ++ userOptions ++ globalOptions
      config = collateOptions(collected)
    } yield validateConfig(config).toEither
  }

  private def findSource(priorityOptions: ConfigOptions): Path =
    priorityOptions.options.foldRight(pwd)((co, f) => co match {
      case ConfigOption.Source(source) => source
      case _ => f
    })

  private def sourceOptions(source: Path): IO[ConfigOptions] =
    readFile(source, ".thorp.conf")

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
    configOptions.options.foldRight(defaultConfig)((co, c) => co.update(c))
}

object ConfigurationBuilder extends ConfigurationBuilder
