package net.kemitix.thorp.config

import net.kemitix.thorp.domain.Sources
import net.kemitix.thorp.filesystem.FileSystem
import zio.ZIO

trait SourceConfigLoader {

  val thorpConfigFileName = ".thorp.conf"

  def loadSourceConfigs
    : Sources => ZIO[FileSystem, List[ConfigValidation], ConfigOptions] =
    sources => {

      val sourceConfigOptions =
        ConfigOptions(sources.paths.map(ConfigOption.Source))

      val reduce: List[ConfigOptions] => ConfigOptions =
        _.foldLeft(sourceConfigOptions) { (acc, co) =>
          acc ++ co
        }

      ZIO
        .foreach(sources.paths) { path =>
          ParseConfigFile.parseFile(path.resolve(thorpConfigFileName))
        }
        .map(reduce)
    }

}

object SourceConfigLoader extends SourceConfigLoader
