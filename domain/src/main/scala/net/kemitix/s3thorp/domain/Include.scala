package net.kemitix.s3thorp.domain

import java.nio.file.Path
import java.util.regex.Pattern

final case class Include(include: String = ".*") {

  private lazy val predicate = Pattern.compile(include).asPredicate

  def isIncluded(path: Path): Boolean = predicate.test(path.toString)

  def isExcluded(path: Path): Boolean = !isIncluded(path)

}

