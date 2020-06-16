package net.kemitix.thorp.config

import java.io.File

import net.kemitix.thorp.domain.Sources
import zio.ZIO

import scala.jdk.CollectionConverters._

trait SourceConfigLoader {

  val thorpConfigFileName = ".thorp.conf"

  def loadSourceConfigs(
      sources: Sources): ZIO[Any, Seq[ConfigValidation], ConfigOptions] =
    ZIO
      .foreach(sources.paths.asScala) { path =>
        ParseConfigFile.parseFile(new File(path.toFile, thorpConfigFileName))
      }
      .map(_.foldLeft(
        ConfigOptions(sources.paths.asScala.map(ConfigOption.Source).toList)) {
        (acc, co) =>
          acc ++ co
      })

}

object SourceConfigLoader extends SourceConfigLoader
