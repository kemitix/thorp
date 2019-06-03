package net.kemitix.s3thorp.domain

import java.nio.file.Path
import java.util.function.Predicate
import java.util.regex.Pattern

final case class Exclude(exclude: String = "!.*") {

  lazy val predicate: Predicate[String] = Pattern.compile(exclude).asPredicate()

  def isIncluded(path: Path): Boolean = !isExcluded(path)

  def isExcluded(path: Path): Boolean = predicate.test(path.toString)

}
