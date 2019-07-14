package net.kemitix.thorp.domain

import java.nio.file.Path

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
case class Sources(paths: List[Path]) {
  def ++(path: Path): Sources = this ++ List(path)
  def ++(otherPaths: List[Path]): Sources = Sources(
    otherPaths.foldLeft(paths)((acc, path) => if (acc.contains(path)) acc else acc ++ List(path))
  )

  /**
    * Returns the source path for the given path.
    *
    * @param path the path to find the matching source
    * @return the source for the path
    * @throws NoSuchElementException if no source matches the path
    */
  def forPath(path: Path): Path =
    paths.find(source => path.startsWith(source)).get
}
