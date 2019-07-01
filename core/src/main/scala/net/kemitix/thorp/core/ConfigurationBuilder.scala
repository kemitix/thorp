package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Paths

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

  private val pwdFile: File = Paths.get(System.getenv("PWD")).toFile

  private val defaultConfig: Config = Config(source = pwdFile)

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

  private def findSource(priorityOptions: ConfigOptions): File =
    priorityOptions.options.foldRight(pwdFile)((co, f) => co match {
      case ConfigOption.Source(source) => source.toFile
      case _ => f
    })

  private def sourceOptions(source: File): IO[ConfigOptions] =
    readFile(source, ".thorp.conf")

  private def userOptions(higherPriorityOptions: ConfigOptions): IO[ConfigOptions] =
    if (ConfigQuery.ignoreUserOptions(higherPriorityOptions)) IO(ConfigOptions())
    else readFile(userHome, ".config/thorp.conf")

  private def globalOptions(higherPriorityOptions: ConfigOptions): IO[ConfigOptions] =
    if (ConfigQuery.ignoreGlobalOptions(higherPriorityOptions)) IO(ConfigOptions())
    else parseFile(Paths.get("/etc/thorp.conf"))

  private def userHome = new File(System.getProperty("user.home"))

  private def readFile(source: File, filename: String): IO[ConfigOptions] =
    parseFile(source.toPath.resolve(filename))

  private def collateOptions(configOptions: ConfigOptions): Config =
    configOptions.options.foldRight(defaultConfig)((co, c) => co.update(c))
}

object ConfigurationBuilder extends ConfigurationBuilder
