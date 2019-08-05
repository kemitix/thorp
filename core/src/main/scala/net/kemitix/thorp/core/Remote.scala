package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{RemoteKey, Sources}
import net.kemitix.thorp.filesystem.FileSystem
import zio.{RIO, ZIO}

object Remote {

  def isMissingLocally(sources: Sources,
                       prefix: RemoteKey,
                       remoteKey: RemoteKey): RIO[FileSystem, Boolean] =
    existsLocally(sources, prefix)(remoteKey)
      .map(exists => !exists)

  def existsLocally(sources: Sources, prefix: RemoteKey)(
      remoteKey: RemoteKey
  ): RIO[FileSystem, Boolean] = {
    def existsInSource(source: Path) =
      RemoteKey.asFile(source, prefix)(remoteKey) match {
        case Some(file) => FileSystem.exists(file)
        case None       => ZIO.succeed(false)
      }
    ZIO
      .foreach(sources.paths)(existsInSource)
      .map(lb => lb.exists(l => l))
  }

}
