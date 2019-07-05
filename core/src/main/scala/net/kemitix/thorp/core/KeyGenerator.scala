package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.RemoteKey

object KeyGenerator {

  def generateKey(source: Path, prefix: RemoteKey)
                 (path: Path): RemoteKey = {
    val relativePath = source.relativize(path.toAbsolutePath)
    RemoteKey(s"${prefix.key}/$relativePath")
  }

}
