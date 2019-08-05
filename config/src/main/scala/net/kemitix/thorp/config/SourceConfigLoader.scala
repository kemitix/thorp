package net.kemitix.thorp.config

import java.io.File

import net.kemitix.thorp.domain.Sources
import net.kemitix.thorp.filesystem.FileSystem
import zio.ZIO

trait SourceConfigLoader {

  val thorpConfigFileName = ".thorp.conf"

  def loadSourceConfigs(
      sources: Sources): ZIO[FileSystem, Seq[ConfigValidation], ConfigOptions] =
    ZIO
      .foreach(sources.paths) { path =>
        ParseConfigFile.parseFile(new File(path.toFile, thorpConfigFileName))
      }
      .map(_.foldLeft(ConfigOptions(sources.paths.map(ConfigOption.Source))) {
        (acc, co) =>
          acc ++ co
      })

}

object SourceConfigLoader extends SourceConfigLoader
