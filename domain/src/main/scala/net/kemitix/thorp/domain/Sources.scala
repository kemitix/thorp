package net.kemitix.thorp.domain

import java.nio.file.Path

/**
  * The paths to synchronise with target.
  *
  * The first source path takes priority over those later in the list,
  * etc. Where there is any file with the same relative path within
  * more than one source, the file in the first listed path is
  * uploaded, and the others are ignored.
  */
case class Sources(paths: List[Path]) {

}
