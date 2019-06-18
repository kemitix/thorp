package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Paths

import cats.data.NonEmptyChain
import net.kemitix.thorp.domain.Config
import net.kemitix.thorp.core.ConfigValidator._

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  private val pwdFile: File = Paths.get(System.getenv("PWD")).toFile

  private val defaultConfig: Config = Config(source = pwdFile)

  def apply(configOptions: Seq[ConfigOption]): Either[NonEmptyChain[ConfigValidation], Config] =
    validateConfig(buildConfig(configOptions)).toEither

  private def buildConfig(configOptions: Seq[ConfigOption]) =
    configOptions.foldRight(defaultConfig)((co, c) => co.update(c))

}

object ConfigurationBuilder extends ConfigurationBuilder
