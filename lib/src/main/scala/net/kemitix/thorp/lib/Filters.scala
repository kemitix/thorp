package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.Filter
import net.kemitix.thorp.domain.Filter.{Exclude, Include}
import zio.ZIO

object Filters {

  def isIncluded(file: File): ZIO[Config, Nothing, Boolean] =
    for {
      filters <- Config.filters
    } yield isIncludedUsingFilters(file.toPath)(filters)

  def isIncludedUsingFilters(p: Path)(filters: List[Filter]): Boolean = {
    sealed trait State
    final case class Unknown()   extends State
    final case class Accepted()  extends State
    final case class Discarded() extends State
    val excluded = isExcludedByFilter(p)(_)
    val included = isIncludedByFilter(p)(_)
    filters.foldRight(Unknown(): State)((filter, state) =>
      (filter, state) match {
        case (_, Accepted())                => Accepted()
        case (_, Discarded())               => Discarded()
        case (x: Exclude, _) if excluded(x) => Discarded()
        case (i: Include, _) if included(i) => Accepted()
        case _                              => Unknown()
    }) match {
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
