package net.kemitix.thorp.domain

import java.util.function.Predicate
import java.util.regex.Pattern

sealed trait Filter {
  def predicate: Predicate[String]
}

object Filter {
  final case class Include(include: String) extends Filter {
    lazy val predicate: Predicate[String] = Pattern.compile(include).asPredicate
  }
  object Include {
    def all: Include = Include(".*")
  }
  final case class Exclude(exclude: String) extends Filter {
    lazy val predicate: Predicate[String] =
      Pattern.compile(exclude).asPredicate()
  }
}
