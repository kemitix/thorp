package net.kemitix.thorp.core

import net.kemitix.thorp.domain.Sources
import zio.IO

trait SourceConfigLoader {

  val thorpConfigFileName = ".thorp.conf"

  def loadSourceConfigs: Sources => IO[List[ConfigValidation], ConfigOptions] =
    sources => {

      val sourceConfigOptions =
        ConfigOptions(sources.paths.map(ConfigOption.Source))

      val reduce: List[ConfigOptions] => ConfigOptions =
        _.foldLeft(sourceConfigOptions) { (acc, co) =>
          acc ++ co
        }

      IO.foreach(sources.paths) { path =>
          ParseConfigFile.parseFile(path.resolve(thorpConfigFileName))
        }
        .map(reduce)
    }

}

object SourceConfigLoader extends SourceConfigLoader
