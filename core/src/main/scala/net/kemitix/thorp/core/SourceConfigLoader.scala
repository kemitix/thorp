package net.kemitix.thorp.core

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.domain.Sources

trait SourceConfigLoader {

  val thorpConfigFileName = ".thorp.conf"

  def loadSourceConfigs: Sources => IO[ConfigOptions] =
    sources => {

      val sourceConfigOptions =
        ConfigOptions(sources.paths.map(ConfigOption.Source))

      val reduce: List[ConfigOptions] => ConfigOptions =
        _.foldLeft(sourceConfigOptions) { (acc, co) => acc ++ co }

      sources.paths
        .map(_.resolve(thorpConfigFileName))
        .map(ParseConfigFile.parseFile).sequence
        .map(reduce)
    }

}

object SourceConfigLoader extends SourceConfigLoader
