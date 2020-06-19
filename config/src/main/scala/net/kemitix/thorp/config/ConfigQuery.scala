package net.kemitix.thorp.config

import java.nio.file.Paths
import scala.jdk.CollectionConverters._

import net.kemitix.thorp.domain.Sources

trait ConfigQuery {

  def showVersion(configOptions: ConfigOptions): Boolean =
    configOptions.options
      .stream()
      .anyMatch(_.isInstanceOf[ConfigOption.Version])

  def batchMode(configOptions: ConfigOptions): Boolean =
    configOptions.options
      .stream()
      .anyMatch(_.isInstanceOf[ConfigOption.BatchMode])

  def ignoreUserOptions(configOptions: ConfigOptions): Boolean =
    configOptions.options
      .stream()
      .anyMatch(_.isInstanceOf[ConfigOption.IgnoreUserOptions])

  def ignoreGlobalOptions(configOptions: ConfigOptions): Boolean =
    configOptions.options
      .stream()
      .anyMatch(_.isInstanceOf[ConfigOption.IgnoreGlobalOptions])

  def sources(configOptions: ConfigOptions): Sources = {
    val explicitPaths = configOptions.options.asScala.flatMap {
      case source: ConfigOption.Source => List(source.path)
      case _                           => List.empty
    }.toList
    val paths = explicitPaths match {
      case List() => List(Paths.get(System.getenv("PWD")))
      case _      => explicitPaths
    }
    Sources.create(paths.asJava)
  }

}

object ConfigQuery extends ConfigQuery
