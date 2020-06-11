package net.kemitix.thorp.filesystem

import java.io.File
import java.nio.file.{Path, Paths}

final case class Resource(
    cls: Object,
    file: String
) {

  def toPath: Path             = Paths.get(cls.getClass.getResource(file).getPath)
  def toFile: File             = toPath.toFile
  def getCanonicalPath: String = toPath.toFile.getCanonicalPath
  def length: Long             = toFile.length()
}
