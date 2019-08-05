package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.Filter
import net.kemitix.thorp.domain.Filter.{Exclude, Include}

object Filters {

  def isIncluded(p: Path)(filters: List[Filter]): Boolean = {
    sealed trait State
    case class Unknown()   extends State
    case class Accepted()  extends State
    case class Discarded() extends State
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
    filter.predicate.test(path.toString)

  def isExcludedByFilter(path: Path)(filter: Filter): Boolean =
    filter.predicate.test(path.toString)

}
