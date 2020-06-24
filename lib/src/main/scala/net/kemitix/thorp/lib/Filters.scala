package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.domain.Filter
import net.kemitix.thorp.domain.Filter.{Exclude, Include}

import scala.jdk.CollectionConverters._

object Filters {

  def isIncluded(configuration: Configuration, file: File): Boolean =
    isIncluded(file.toPath)(configuration.filters.asScala.toList)

  def isIncluded(p: Path)(filters: List[Filter]): Boolean = {
    sealed trait State
    final case class Unknown() extends State
    final case class Accepted() extends State
    final case class Discarded() extends State
    val excluded = isExcludedByFilter(p)(_)
    val included = isIncludedByFilter(p)(_)
    filters.foldRight(Unknown(): State)(
      (filter, state) =>
        (filter, state) match {
          case (_, Accepted())                => Accepted()
          case (_, Discarded())               => Discarded()
          case (x: Exclude, _) if excluded(x) => Discarded()
          case (i: Include, _) if included(i) => Accepted()
          case _                              => Unknown()
      }
    ) match {
      case Accepted()  => true
      case Discarded() => false
      case Unknown() =>
        filters.forall {
          case _: Include => false
          case _          => true
        }
    }
  }

  def isIncludedByFilter(path: Path)(filter: Filter): Boolean =
    filter.predicate.test(path.toFile.getPath)

  def isExcludedByFilter(path: Path)(filter: Filter): Boolean =
    filter.predicate.test(path.toFile.getPath)

}
