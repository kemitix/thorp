package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Paths

import cats.data.{NonEmptyChain, Validated, ValidatedNec}
import net.kemitix.thorp.domain.Config

/**
  * Builds a configuration from settings in a file within the
  * `source` directory and from supplied configuration options.
  */
trait ConfigurationBuilder {

  val defaultConfig: Config =
    Config(source = Paths.get(System.getenv("PWD")).toFile)

  def apply(configOptions: Seq[ConfigOption]): Either[NonEmptyChain[ConfigValidation], Config] = {
    val config = configOptions.foldRight(defaultConfig)((co, c) => co.update(c))
    ConfigValidator.validateConfig(config).toEither
  }

}

object ConfigurationBuilder extends ConfigurationBuilder