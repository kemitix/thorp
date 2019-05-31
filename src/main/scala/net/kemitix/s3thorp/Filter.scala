package net.kemitix.s3thorp

import java.nio.file.Path
import java.util.function.Predicate
import java.util.regex.Pattern

final case class Filter(filter: String = ".*") {

  lazy val predicate: Predicate[String] = Pattern.compile(filter).asPredicate.negate

  def isIncluded(path: Path): Boolean = !isExcluded(path)

  def isExcluded(path: Path): Boolean = predicate.test(path.toString)

}

