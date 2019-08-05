package net.kemitix.thorp.domain

import java.nio.file.Path

import zio.{Task, ZIO}

/**
  * The paths to synchronise with target.
  *
  * The first source path takes priority over those later in the list,
  * etc. Where there is any file with the same relative path within
  * more than one source, the file in the first listed path is
  * uploaded, and the others are ignored.
  *
  * A path should only occur once in paths.
  */
case class Sources(
    paths: List[Path]
) {
  def +(path: Path)(implicit m: Monoid[Sources]): Sources = this ++ List(path)
  def ++(otherPaths: List[Path])(implicit m: Monoid[Sources]): Sources =
    m.op(this, Sources(otherPaths))
}

object Sources {
  final val emptySources = Sources(List.empty)
  implicit def sourcesAppendMonoid: Monoid[Sources] = new Monoid[Sources] {
    override def zero: Sources = emptySources
    override def op(t1: Sources, t2: Sources): Sources =
      Sources(t2.paths.foldLeft(t1.paths) { (acc, path) =>
        if (acc.contains(path)) acc
        else acc ++ List(path)
      })
  }

  /**
    * Returns the source path for the given path.
    */
  def forPath(path: Path)(sources: Sources): Task[Path] =
    ZIO
      .fromOption(sources.paths.find(s => path.startsWith(s)))
      .mapError(_ => new Exception("Path is not within any known source"))
}
