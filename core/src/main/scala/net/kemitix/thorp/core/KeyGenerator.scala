package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{RemoteKey, Sources}
import zio.Task

object KeyGenerator {

  def generateKey(
      sources: Sources,
      prefix: RemoteKey
  )(path: Path): Task[RemoteKey] =
    Sources
      .forPath(path)(sources)
      .map(_.relativize(path.toAbsolutePath))
      .map(_.toFile.getPath)
      .map(RemoteKey.resolve(_)(prefix))

}
