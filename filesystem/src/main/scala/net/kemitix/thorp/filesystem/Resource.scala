package net.kemitix.thorp.filesystem

import java.nio.file.Paths

final case class Resource(
    cls: Object,
    file: String
) {
  def toPath = Paths.get(cls.getClass.getResource(file).getPath)
}
