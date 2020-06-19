package net.kemitix.thorp.config

import java.io.File
import java.util

import net.kemitix.thorp.domain.Sources

import scala.jdk.CollectionConverters._

trait SourceConfigLoader {

  val thorpConfigFileName = ".thorp.conf"

  def loadSourceConfigs(sources: Sources): ConfigOptions =
    sources
      .paths()
      .asScala
      .toList
      .map(path =>
        ParseConfigFile.parseFile(new File(path.toFile, thorpConfigFileName)))
      .foldLeft(ConfigOptions(sourceConfigOptions(sources)))((acc, co) =>
        acc ++ co)

  private def sourceConfigOptions(
      sources: Sources): java.util.List[ConfigOption] = {
    val options: util.ArrayList[ConfigOption] = new util.ArrayList[ConfigOption]
    sources
      .paths()
      .stream()
      .map(path => ConfigOption.Source(path))
      .forEach(o => options.add(o))
    options
  }
}

object SourceConfigLoader extends SourceConfigLoader
