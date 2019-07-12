package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{RemoteKey, Sources}

object KeyGenerator {

  def generateKey(sources: Sources,
                  prefix: RemoteKey)
                 (path: Path): RemoteKey = {
    val source = sources.forPath(path)
    val relativePath = source.relativize(path.toAbsolutePath)
    RemoteKey(List(prefix.key, relativePath.toString)
        .filterNot(_.isEmpty)
        .mkString("/"))
  }

}
