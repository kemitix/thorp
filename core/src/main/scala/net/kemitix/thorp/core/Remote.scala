package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{RemoteKey, Sources}
import net.kemitix.thorp.filesystem.FileSystem
import zio.{TaskR, ZIO}

object Remote {

  def isMissingLocally(sources: Sources, prefix: RemoteKey)(
      remoteKey: RemoteKey
  ): TaskR[FileSystem, Boolean] =
    existsLocally(sources, prefix)(remoteKey)
      .map(exists => !exists)

  def existsLocally(sources: Sources, prefix: RemoteKey)(
      remoteKey: RemoteKey
  ): TaskR[FileSystem, Boolean] = {
    def existsInSource(source: Path) =
      remoteKey.asFile(source, prefix) match {
        case Some(file) => FileSystem.exists(file)
        case None       => ZIO.succeed(false)
      }
    ZIO
      .foreach(sources.paths)(existsInSource)
      .map(lb => lb.exists(l => l))
  }

}
