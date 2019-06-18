package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Paths

import cats.data.NonEmptyChain
import net.kemitix.thorp.core.ConfigValidator._
import net.kemitix.thorp.domain.Config

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val pwdFile: File = Paths.get(System.getenv("PWD")).toFile

  private val defaultConfig: Config = Config(source = pwdFile)

  def apply(priorityOptions: Seq[ConfigOption]): Either[NonEmptyChain[ConfigValidation], Config] =
    validateConfig(buildConfig(collectConfigOptions(priorityOptions))).toEither

  private def buildConfig(configOptions: Seq[ConfigOption]) =
    configOptions.foldRight(defaultConfig)((co, c) => co.update(c))

  private def collectConfigOptions(priorityOptions: Seq[ConfigOption]) =
    priorityOptions ++ sourceDirConfigOptions(findSource(priorityOptions))

  def findSource(priorityOptions: Seq[ConfigOption]): File =
    (priorityOptions flatMap {
      case ConfigOption.Source(source) => Option(source.toFile)
      case _ => None
    }).headOption.getOrElse(pwdFile)

  def sourceDirConfigOptions(source: File): Seq[ConfigOption] =
    readOptionsFromFile(source, ".thorp.yaml")

  def readOptionsFromFile(source: File, filename: String): Seq[ConfigOption] =
    ParseConfigFile(source.toPath.resolve(filename).toFile)
}

object ConfigurationBuilder extends ConfigurationBuilder
