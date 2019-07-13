package net.kemitix.thorp.core

import java.nio.file.{Files, Path, Paths}

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.ConfigValidator.validateConfig
import net.kemitix.thorp.core.ParseConfigFile.parseFile
import net.kemitix.thorp.domain.{Config, Sources}

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val thorpConfigFileName = ".thorp.conf"

  def buildConfig(priorityOptions: ConfigOptions): IO[Either[NonEmptyChain[ConfigValidation], Config]] = {
    val sources = ConfigQuery.sources(priorityOptions)
    for {
      sourceOptions <- sourceOptions(sources)
      userOptions <- userOptions(priorityOptions ++ sourceOptions)
      globalOptions <- globalOptions(priorityOptions ++ sourceOptions ++ userOptions)
      collected = priorityOptions ++ sourceOptions ++ userOptions ++ globalOptions
      config = collateOptions(collected)
    } yield validateConfig(config).toEither
  }

  private def sourceOptions(sources: Sources): IO[ConfigOptions] = {
    def existingThorpConfigFiles(sources: Sources) =
      sources.paths
        .map(_.resolve(thorpConfigFileName))
        .filter(Files.exists(_))

    def filterForSources: IO[ConfigOptions] => IO[(Sources, ConfigOptions)] =
      for {configOptions <- _} yield (ConfigQuery.sources(configOptions), configOptions)

    def recurseIntoSources: IO[(Sources, ConfigOptions)] => IO[ConfigOptions] =
      ioSourcesConfigOptions =>
        for {
          sourcesConfigOptions <- ioSourcesConfigOptions
          (sources, configOptions) = sourcesConfigOptions
          moreSourcesConfigOptions <- filterForSources(sourceOptions(sources))
          (_, moreConfigOptions) = moreSourcesConfigOptions
        } yield configOptions ++ moreConfigOptions

    def emptyConfig: IO[ConfigOptions] = IO.pure(ConfigOptions())

    def collectConfigOptions: (IO[ConfigOptions], IO[ConfigOptions]) => IO[ConfigOptions] =
      (ioConfigOptions, ioAcc) =>
        for {
          configOptions <- ioConfigOptions
          acc <- ioAcc
        } yield configOptions ++ acc

    existingThorpConfigFiles(sources)
      .map(ParseConfigFile.parseFile)
      .map(filterForSources)
      .map(recurseIntoSources)
      .foldRight(emptyConfig)(collectConfigOptions)
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

  private def collateOptions(configOptions: ConfigOptions): Config = {
    val pwd = Paths.get(System.getenv("PWD"))
    val initialSource =
      if (noSourcesProvided(configOptions)) List(pwd) else List()
    val initialConfig = Config(sources = Sources(initialSource))
    configOptions.options.foldLeft(initialConfig)((c, co) => co.update(c))
  }

  private def noSourcesProvided(configOptions: ConfigOptions) = {
    ConfigQuery.sources(configOptions).paths.isEmpty
  }
}

object ConfigurationBuilder extends ConfigurationBuilder
