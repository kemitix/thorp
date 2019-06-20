package net.kemitix.thorp.domain

import java.nio.file.Path
import java.util.regex.Pattern

sealed trait Filter

object Filter {

  case class Include(include: String = ".*") extends Filter {

    private lazy val predicate = Pattern.compile(include).asPredicate

    def isIncluded(path: Path): Boolean = predicate.test(path.toString)

  }

  case class Exclude(exclude: String) extends Filter {

    private lazy val predicate = Pattern.compile(exclude).asPredicate()

    def isExcluded(path: Path): Boolean = predicate.test(path.toString)

  }

  def isIncluded(filters: List[Filter])(p: Path): Boolean = {
    sealed trait State
    case class Unknown() extends State
    case class Accepted() extends State
    case class Discarded() extends State
    filters.foldRight(Unknown(): State)((filter, state) =>
      (filter, state) match {
        case (_, Accepted()) => Accepted()
        case (_, Discarded()) => Discarded()
        case (x: Exclude, _) if x.isExcluded(p) => Discarded()
        case (i: Include, _) if i.isIncluded(p) => Accepted()
        case _ => Unknown()
      }) match {
      case Accepted() => true
      case Discarded() => false
      case Unknown() => filters.forall {
        case _: Include => false
        case _ => true
      }
    }
  }

}
